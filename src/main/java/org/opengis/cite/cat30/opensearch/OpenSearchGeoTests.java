package org.opengis.cite.cat30.opensearch;

import com.sun.jersey.api.client.ClientResponse;
import org.geotoolkit.geometry.Envelopes;
import org.geotoolkit.geometry.GeneralEnvelope;
import org.geotoolkit.referencing.crs.DefaultGeographicCRS;
import org.opengis.cite.cat30.*;
import org.opengis.cite.cat30.util.*;
import org.opengis.cite.geomatics.Extents;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.operation.TransformException;
import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.ws.rs.core.MediaType;
import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.xpath.XPathExpressionException;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Verifies behavior of the SUT when processing OpenSearch requests that contain
 * geographic extensions defined in <em>OGC OpenSearch Geo and Time
 * Extensions</em> (OGC 10-032r8). The relevant namespace URI is
 * <code>http://a9.com/-/opensearch/extensions/geo/1.0/</code>.
 * <p>
 * <p>
 * All implementations must satisfy the requirements of the
 * <strong>Core</strong> conformance class (see OGC 10-032r8, Table 1). A
 * conforming service must:</p>
 * <ul>
 * <li>present a valid OpenSearch description document;</li>
 * <li>define a URL template for the Atom response type;</li>
 * <li>implement a bounding box search (<code>geo:box</code>).</li>
 * </ul>
 * <p>
 * <p>
 * In an Atom feed or entry, a bounding box is represented using a GeoRSS
 * element. Either the simple or GML variant is acceptable:</p>
 * <ul>
 * <li><code>georss:box</code> (EPSG 4326)</li>
 * <li><code>georss:where/{http://www.opengis.net/gml}Envelope</code> (any
 * CRS)</li>
 * </ul>
 * <p>
 * <p>
 * In addition to the service capabilities listed above, a Catalog v3 service
 * must also implement the <strong>Get record by id</strong> conformance class
 * that allows a client to retrieve a record by identifier
 * (<code>geo:uid</code>). This requirement is a consequence of applying the
 * <code>Filter-FES-KVP</code> conformance class to OpenSearch queries (see
 * Table 1).</p>
 * <p>
 * <p style="margin-bottom: 0.5em"><strong>Sources</strong></p>
 * <ul>
 * <li><a href="https://portal.opengeospatial.org/files/?artifact_id=56866&version=2"
 * target="_blank">OGC 10-032r8</a>: OGC OpenSearch Geo and Time Extensions,
 * Version 1.0.0</li>
 * <li><a href="http://www.georss.org/" target="_blank">GeoRSS</a></li>
 * </ul>
 */
public class OpenSearchGeoTests extends CommonFixture {

    static final QName UID_PARAM = new QName(Namespaces.OS_GEO, "uid");
    private Document openSearchDescr;
    private List<Node> urlTemplates;
    /**
     * A list of record identifiers retrieved from the SUT.
     */
    private List<String> idList;
    /**
     * An Envelope defining the total geographic extent of the sample data.
     */
    private Envelope geoExtent;

    /**
     * Initializes the test fixture. A Document representing an OpenSearch
     * description document is obtained from the test context and the URL
     * templates it contains are extracted.
     *
     * @param testContext The test context containing various suite attributes.
     */
    @BeforeClass
    public void initOpenSearchGeoTestsFixture(ITestContext testContext) {
        this.openSearchDescr = (Document) testContext.getSuite().getAttribute(
                SuiteAttribute.OPENSEARCH_DESCR.getName());
        if (null == this.openSearchDescr) {
            throw new SkipException("OpenSearch description not found in test context.");
        }
        this.urlTemplates = ServiceMetadataUtils.getOpenSearchURLTemplates(
                this.openSearchDescr);
        DatasetInfo dataset = (DatasetInfo) testContext.getSuite().getAttribute(
                SuiteAttribute.DATASET.getName());
        if (null == dataset) {
            throw new SkipException("Dataset info not found in test context.");
        }
        this.idList = dataset.getRecordIdentifiers();
        this.geoExtent = dataset.getGeographicExtent();
    }

    /**
     * [Test] Submits an OpenSearch request that includes a (randomly generated)
     * identifier for which there is no matching record. Status code 404 (Not
     * Found) is expected in response. An error message may be conveyed in the
     * response entity.
     */
    @Test(description = "Requirement-141")
    public void getResourceById_notFound() {
        QName uidParam = new QName(Namespaces.OS_GEO, "uid");
        List<Node> uidTemplates = OpenSearchTemplateUtils.filterURLTemplatesByParam(
                this.urlTemplates, uidParam);
        Assert.assertFalse(uidTemplates.isEmpty(),
                "No URL templates containing {geo:uid} parameter.");
        Map<QName, String> values = new HashMap<>();
        values.put(uidParam, "uid-" + UUID.randomUUID().toString());
        for (Node urlTemplate : uidTemplates) {
            Element urlElem = (Element) urlTemplate;
            String mediaType = urlElem.getAttribute("type");
            if (!mediaType.contains("xml")) {
                continue; // ignore non-XML media types
            }
            URI uri = OpenSearchTemplateUtils.buildRequestURI(urlElem, values);
            request = ClientUtils.buildGetRequest(uri, null,
                    MediaType.valueOf(mediaType));
            response = this.client.handle(request);
            Assert.assertEquals(response.getStatus(),
                    ClientResponse.Status.NOT_FOUND.getStatusCode(),
                    ErrorMessage.get(ErrorMessageKeys.UNEXPECTED_STATUS));
        }
    }

    /**
     * [Test] Submits an OpenSearch request that includes a resource identifier.
     * A matching entry is expected in the response, along with the os:Query
     * element that describes the search request. The request URI is constructed
     * in accord with a URL template containing the <code>{geo:uid}</code>
     * parameter. Any other template parameters are set to their default values.
     *
     * @see "OGC OpenSearch Geo and Time Extensions (OGC 10-032r8), 9.3.2:
     * Normal response XML encoding"
     */
    @Test(description = "OGC 12-176r6, Table 6")
    public void getResourceById() throws XPathExpressionException {
        List<Node> uidTemplates = OpenSearchTemplateUtils.filterURLTemplatesByParam(
                this.urlTemplates, UID_PARAM);
        Assert.assertFalse(uidTemplates.isEmpty(),
                "No URL templates containing {geo:uid} parameter.");
        Map<QName, String> values = new HashMap<>();
        int randomIndex = ThreadLocalRandom.current().nextInt(this.idList.size());
        String id = this.idList.get(randomIndex);
        values.put(UID_PARAM, URIUtils.getPercentEncodedString(id));
        for (Node urlTemplate : uidTemplates) {
            Element url = (Element) urlTemplate;
            String mediaType = url.getAttribute("type");
            if (!mediaType.startsWith(MediaType.APPLICATION_ATOM_XML)) {
                continue;
            }
            URI uri = OpenSearchTemplateUtils.buildRequestURI(url, values);
            request = ClientUtils.buildGetRequest(uri, null,
                    MediaType.valueOf(mediaType));
            response = this.client.handle(request);
            Assert.assertEquals(response.getStatus(),
                    ClientResponse.Status.OK.getStatusCode(),
                    ErrorMessage.get(ErrorMessageKeys.UNEXPECTED_STATUS));
            Document entity = getResponseEntityAsDocument(response, null);
            String recordPath = "atom:feed/atom:entry";
            String expr = String.format("/%s/dc:identifier = '%s'",
                    recordPath, id);
            Map<String, String> namespaceBindings = new HashMap<>();
            namespaceBindings.put(Namespaces.DCMES, "dc");
            namespaceBindings.put(Namespaces.ATOM, "atom");
            ETSAssert.assertXPath(expr, entity,namespaceBindings);
            Source source = ClientUtils.getResponseEntityAsSource(response, null);
            URL schemaUrl = getClass().getResource(SCHEMATRON_ATOM);
            ETSAssert.assertSchematronValid(schemaUrl, source);
        }
    }

    /**
     * [Test] Submits an OpenSearch request that includes a bounding box with
     * invalid (UTM) coordinates. Status code 400 (Bad Request) is expected in
     * response. An error message may be conveyed in the response entity.
     */
    @Test(description = "OGC 10-032r8: 9.3,A.3")
    public void invalidBoundingBoxCoords() {
        QName boxParam = new QName(Namespaces.OS_GEO, "box");
        List<Node> boxTemplates = OpenSearchTemplateUtils.filterURLTemplatesByParam(
                this.urlTemplates, boxParam);
        Assert.assertFalse(boxTemplates.isEmpty(),
                "No URL templates containing {geo:box} parameter.");
        Map<QName, String> values = new HashMap<>();
        values.put(boxParam, "514432,5429689,529130,5451619");
        for (Node urlTemplate : boxTemplates) {
            Element url = (Element) urlTemplate;
            String mediaType = url.getAttribute("type");
            if (!mediaType.contains("xml")) {
                continue; // ignore non-XML media types
            }
            URI uri = OpenSearchTemplateUtils.buildRequestURI(url, values);
            request = ClientUtils.buildGetRequest(uri, null,
                    MediaType.valueOf(mediaType));
            response = this.client.handle(request);
            Assert.assertEquals(response.getStatus(),
                    ClientResponse.Status.BAD_REQUEST.getStatusCode(),
                    ErrorMessage.get(ErrorMessageKeys.UNEXPECTED_STATUS));
        }
    }

    /**
     * [Test] Submits an OpenSearch request that includes a bounding box in the
     * normative CRS ("urn:ogc:def:crs:OGC:1.3:CRS84"). The request URI is
     * constructed in accord with a URL template containing the
     * <code>{geo:box}</code> parameter. Any other template parameters are set
     * to their default values.
     * <p>
     * <p>
     * All matching record representations in the response entity must satisfy
     * the bounding box constraint. If the entity is an Atom feed, it must also
     * contain the os:Query element that describes the search request.
     * </p>
     *
     * @see "OGC OpenSearch Geo and Time Extensions (OGC 10-032r8), 9.3.2:
     * Normal response XML encoding"
     */
    @Test(description = "Requirements: 022,023; OGC 10-032r8, A.3")
    public void boundingBoxQuery() {
        QName boxParam = new QName(Namespaces.OS_GEO, "box");
        List<Node> boxTemplates = OpenSearchTemplateUtils.filterURLTemplatesByParam(
                this.urlTemplates, boxParam);
        Assert.assertFalse(boxTemplates.isEmpty(),
                "No URL templates containing {geo:box} parameter.");
        Map<QName, String> values = new HashMap<>();
        Envelope bbox = this.geoExtent;
        try {
            if (!bbox.getCoordinateReferenceSystem().equals(
                    DefaultGeographicCRS.WGS84)) {
                bbox = new GeneralEnvelope(Envelopes.transform(bbox,
                        DefaultGeographicCRS.WGS84));
            }
        } catch (TransformException ex) {
            throw new AssertionError("Failed to create CRS84 box from envelope in source CRS: "
                    + bbox.getCoordinateReferenceSystem().getName(), ex);
        }
        values.put(boxParam, Extents.envelopeToString(bbox));
        for (Node urlTemplate : boxTemplates) {
            Element url = (Element) urlTemplate;
            String mediaType = url.getAttribute("type");
            if (!mediaType.contains("xml")) {
                continue; // ignore non-XML media types
            }
            URI uri = OpenSearchTemplateUtils.buildRequestURI(url, values);
            request = ClientUtils.buildGetRequest(uri, null,
                    MediaType.valueOf(mediaType));
            response = this.client.handle(request);
            Assert.assertEquals(response.getStatus(),
                    ClientResponse.Status.OK.getStatusCode(),
                    ErrorMessage.get(ErrorMessageKeys.UNEXPECTED_STATUS));
            Document entity = getResponseEntityAsDocument(response, null);
            ETSAssert.assertEnvelopeIntersectsBoundingBoxes(bbox,
                    new DOMSource(entity));
            if (mediaType.startsWith(MediaType.APPLICATION_ATOM_XML)) {
                Source source = ClientUtils.getResponseEntityAsSource(response, null);
                URL schemaUrl = getClass().getResource(SCHEMATRON_ATOM);
                ETSAssert.assertSchematronValid(schemaUrl, source);
            } else if (mediaType.startsWith("application/rss+xml")) {
                QName docElem = new QName(XMLConstants.NULL_NS_URI, "rss");
                ETSAssert.assertQualifiedName(entity.getDocumentElement(), docElem);
            } else {
                QName docElem = new QName(Namespaces.CSW, CAT3.GET_RECORDS_RSP);
                ETSAssert.assertQualifiedName(entity.getDocumentElement(), docElem);
            }
        }
    }
}
