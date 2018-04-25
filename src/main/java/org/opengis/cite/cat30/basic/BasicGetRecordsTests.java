package org.opengis.cite.cat30.basic;

import com.sun.jersey.api.client.ClientResponse;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Validator;
import org.opengis.cite.cat30.CAT3;
import org.opengis.cite.cat30.CommonFixture;
import org.opengis.cite.cat30.ETSAssert;
import org.opengis.cite.cat30.ErrorMessage;
import org.opengis.cite.cat30.ErrorMessageKeys;
import org.opengis.cite.cat30.Namespaces;
import org.opengis.cite.cat30.SuiteAttribute;
import org.opengis.cite.cat30.util.ClientUtils;
import org.opengis.cite.cat30.util.DatasetInfo;
import org.opengis.cite.cat30.util.ServiceMetadataUtils;
import org.opengis.cite.cat30.util.XMLUtils;
import org.opengis.cite.validation.ValidationErrorHandler;
import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * Provides tests that apply to the <code>GetRecords</code> request using the
 * KVP syntax.
 *
 * <p>
 * The KVP syntax must be supported; this encoding is generally used with the
 * GET method but may also be used with the POST method; however a POST method
 * binding is very unusual and will be advertised in the capabilities document
 * as an operational constraint as indicated below. The media type of a KVP
 * request entity is "application/x-www-form-urlencoded".
 * </p>
 *
 * @see "OGC 12-176r6, 7.3: GetRecords operation"
 */
public class BasicGetRecordsTests extends CommonFixture {

    /**
     * Service endpoint for GetRecords using the GET method.
     */
    private URI getURI;
    /**
     * Service endpoint for GetRecords using the POST method.
     */
    private URI postURI;
    /**
     * Information about the sample data retrieved from the IUT.
     */
    private DatasetInfo datasetInfo;

    void setGetEndpoint(URI uri) {
        this.getURI = uri;
    }

    /**
     * Finds the GET and POST method endpoints for the GetRecords request in the
     * capabilities document.
     *
     * @param testContext The test context containing various suite attributes.
     */
    @BeforeClass
    public void findRequestEndpoints(ITestContext testContext) {
        this.getURI = ServiceMetadataUtils.getOperationEndpoint(
                this.cswCapabilities, CAT3.GET_RECORDS, HttpMethod.GET);
        if (null == this.getURI.getScheme()) {
            throw new SkipException("GET endpoint for GetRecords request not found.");
        }
        this.postURI = ServiceMetadataUtils.getOperationEndpoint(
                this.cswCapabilities, CAT3.GET_RECORDS, HttpMethod.POST);
    }

    /**
     * Gets information about the sample data obtained from the IUT, including:
     * its total geographic extent; a collection of record titles; and a set of
     * record identifiers. Each csw:Record element may contain at least one
     * ows:BoundingBox (or ows:WGS84BoundingBox) element that describes the
     * spatial coverage of a catalogued resource.
     *
     * @param testContext The test context containing various suite attributes.
     */
    @BeforeClass
    public void getDatasetInfo(ITestContext testContext) {
        DatasetInfo dataset = (DatasetInfo) testContext.getSuite().getAttribute(
                SuiteAttribute.DATASET.getName());
        if (null == dataset) {
            throw new SkipException("Dataset info not found in test context.");
        }
        this.datasetInfo = dataset;
    }

    /**
     * [Test] Submits a GetRecords request with no search criteria. The
     * <code>namespace</code> parameter declares a namespace binding for the
     * common type name (tns:Record). The results must contain one or more
     * csw:BriefRecord elements.
     *
     * <p style="margin-bottom: 0.5em"><strong>Sources</strong></p>
     * <ul>
     * <li>OGC 12-176r6, Table 19: KVP encoding for GetRecords operation
     * request</li>
     * <li>OGC 12-176r6, 7.3.4.1: NAMESPACE parameter</li>
     * </ul>
     */
    @Test(description = "Requirements: 063")
    public void getBriefRecordsWithNamespaceBinding() {
        Map<String, String> qryParams = new HashMap<>();
        qryParams.put(CAT3.REQUEST, CAT3.GET_RECORDS);
        qryParams.put(CAT3.SERVICE, CAT3.SERVICE_TYPE_CODE);
        qryParams.put(CAT3.VERSION, CAT3.VERSION_3_0_0);
        qryParams.put(CAT3.NAMESPACE, String.format("xmlns(tns=%s)", Namespaces.CSW));
        qryParams.put(CAT3.TYPE_NAMES, "tns:Record");
        qryParams.put(CAT3.ELEMENT_SET, CAT3.ELEMENT_SET_BRIEF);
        request = ClientUtils.buildGetRequest(this.getURI, qryParams,
                MediaType.APPLICATION_XML_TYPE);
        response = this.client.handle(request);
        Assert.assertEquals(response.getStatus(),
                ClientResponse.Status.OK.getStatusCode(),
                ErrorMessage.get(ErrorMessageKeys.UNEXPECTED_STATUS));
        Document entity = getResponseEntityAsDocument(response, null);
        String expr = "count(//csw:SearchResults/csw:BriefRecord) > 0";
        ETSAssert.assertXPath(expr, entity, null);
    }

    /**
     * [Test] Submits a GetRecords request with no search criteria. The
     * requested record representation is csw:SummaryRecord; the
     * csw:SearchResults element in the response cannot be empty.
     */
    @Test(description = "Requirements: 101")
    public void getSummaryRecordsInDefaultRepresentation() {
        Map<String, String> qryParams = new HashMap<>();
        qryParams.put(CAT3.REQUEST, CAT3.GET_RECORDS);
        qryParams.put(CAT3.SERVICE, CAT3.SERVICE_TYPE_CODE);
        qryParams.put(CAT3.VERSION, CAT3.VERSION_3_0_0);
        qryParams.put(CAT3.TYPE_NAMES, "Record");
        qryParams.put(CAT3.ELEMENT_SET, CAT3.ELEMENT_SET_SUMMARY);
        request = ClientUtils.buildGetRequest(this.getURI, qryParams,
                MediaType.APPLICATION_XML_TYPE);
        response = this.client.handle(request);
        Assert.assertEquals(response.getStatus(),
                ClientResponse.Status.OK.getStatusCode(),
                ErrorMessage.get(ErrorMessageKeys.UNEXPECTED_STATUS));
        Document entity = getResponseEntityAsDocument(response, null);
        String expr = "count(//csw:SearchResults/csw:SummaryRecord) > 0";
        ETSAssert.assertXPath(expr, entity, null);
    }

    /**
     * [Test] Submits a GetRecords request with no search criteria. The Accept
     * header and the outputFormat parameter express a preference for an Atom
     * feed in the response. The resulting atom:feed must be valid according to
     * RFC 4287.
     *
     * <p style="margin-bottom: 0.5em"><strong>Sources</strong></p>
     * <ul>
     * <li>OGC 12-176r6, 7.3.6: Atom response</li>
     * <li>OGC 10-032r8, Table 6: Elements of Search operation response in the
     * atom:feed element describing the search service</li>
     * <li><a href="https://tools.ietf.org/html/rfc4287" target="_blank">RFC
     * 4287</a>: The Atom Syndication Format</li>
     * </ul>
     */
    @Test(description = "Requirements: 80,122")
    public void getSummaryRecordsInAtomFeed() {
        Map<String, String> qryParams = new HashMap<>();
        qryParams.put(CAT3.REQUEST, CAT3.GET_RECORDS);
        qryParams.put(CAT3.SERVICE, CAT3.SERVICE_TYPE_CODE);
        qryParams.put(CAT3.VERSION, CAT3.VERSION_3_0_0);
        qryParams.put(CAT3.TYPE_NAMES, "Record");
        qryParams.put(CAT3.ELEMENT_SET, CAT3.ELEMENT_SET_SUMMARY);
        qryParams.put(CAT3.OUTPUT_FORMAT, MediaType.APPLICATION_ATOM_XML);
        request = ClientUtils.buildGetRequest(this.getURI, qryParams,
                MediaType.APPLICATION_ATOM_XML_TYPE);
        response = this.client.handle(request);
        Assert.assertEquals(response.getStatus(),
                ClientResponse.Status.OK.getStatusCode(),
                ErrorMessage.get(ErrorMessageKeys.UNEXPECTED_STATUS));
        Assert.assertEquals(ClientUtils.removeParameters(response.getType()),
                MediaType.APPLICATION_ATOM_XML_TYPE,
                ErrorMessage.get(ErrorMessageKeys.UNEXPECTED_MEDIA_TYPE));
        Document entity = getResponseEntityAsDocument(response, null);
        Map<String, String> nsBindings = Collections.singletonMap(Namespaces.ATOM, "atom");
        String expr = "count(/atom:feed/atom:entry) > 0";
        ETSAssert.assertXPath(expr, entity, nsBindings);
        Validator atomValidator = this.atomSchema.newValidator();
        ValidationErrorHandler err = new ValidationErrorHandler();
        atomValidator.setErrorHandler(err);
        try {
            Source src = XMLUtils.toStreamSource(new DOMSource(entity));
            atomValidator.validate(src);
        } catch (SAXException | IOException ex) {
            Logger.getLogger(GetRecordByIdTests.class.getName()).log(
                    Level.WARNING, "Error attempting to validate Atom feed.", ex);
        }
        Assert.assertFalse(err.errorsDetected(),
                ErrorMessage.format(ErrorMessageKeys.NOT_SCHEMA_VALID,
                        err.getErrorCount(), err.toString()));
    }

    /**
     * [Test] Submits a GetRecords request that specifies an unsupported output
     * format (<code>text/example</code>) as the value of the outputFormat
     * parameter. An exception report is expected in response with HTTP status
     * code 400 and exception code
     * "{@value org.opengis.cite.cat30.CAT3#INVALID_PARAM_VAL}".
     */
    @Test(description = "Requirements: 035,037,042")
    public void getRecordsInUnsupportedOutputFormat() {
        Map<String, String> qryParams = new HashMap<>();
        qryParams.put(CAT3.REQUEST, CAT3.GET_RECORDS);
        qryParams.put(CAT3.SERVICE, CAT3.SERVICE_TYPE_CODE);
        qryParams.put(CAT3.VERSION, CAT3.VERSION_3_0_0);
        qryParams.put(CAT3.TYPE_NAMES, "Record");
        qryParams.put(CAT3.ELEMENT_SET, CAT3.ELEMENT_SET_FULL);
        qryParams.put(CAT3.OUTPUT_FORMAT, "text/example");
        request = ClientUtils.buildGetRequest(this.getURI, qryParams,
                MediaType.WILDCARD_TYPE);
        response = this.client.handle(request);
        ETSAssert.assertExceptionReport(response, CAT3.INVALID_PARAM_VAL,
                CAT3.OUTPUT_FORMAT);
    }

    /**
     * [Test] Submits a GetRecords request that specifies an unknown
     * <code>typeName</code> parameter value. Since the type does not belong to
     * any supported information model, an exception report is expected in
     * response with HTTP status code 400 and exception code
     * "{@value org.opengis.cite.cat30.CAT3#INVALID_PARAM_VAL}".
     *
     * @see "OGC Catalogue Services 3.0 Specification - HTTP Protocol Binding,
     * 7.3.4.7: typeNames parameter"
     */
    @Test(description = "Requirements: 088")
    public void getRecordsWithUnknownTypeName() {
        Map<String, String> qryParams = new HashMap<>();
        qryParams.put(CAT3.REQUEST, CAT3.GET_RECORDS);
        qryParams.put(CAT3.SERVICE, CAT3.SERVICE_TYPE_CODE);
        qryParams.put(CAT3.VERSION, CAT3.VERSION_3_0_0);
        qryParams.put(CAT3.TYPE_NAMES, "UnknownType");
        request = ClientUtils.buildGetRequest(this.getURI, qryParams,
                MediaType.APPLICATION_XML_TYPE);
        response = this.client.handle(request);
        ETSAssert.assertExceptionReport(response, CAT3.INVALID_PARAM_VAL,
                CAT3.TYPE_NAMES);
    }

    /**
     * [Test] Submits a GetRecords request that specifies an unknown
     * <code>elementSetName</code> parameter value. An exception report is
     * expected in response with HTTP status code 400 and exception code
     * "{@value org.opengis.cite.cat30.CAT3#INVALID_PARAM_VAL}".
     *
     * @see "OGC Catalogue Services 3.0 Specification - HTTP Protocol Binding,
     * 7.3.4.8: ElementName or ElementSetName parameter"
     */
    @Test(description = "Requirements: 102")
    public void unknownElementSetName() {
        Map<String, String> qryParams = new HashMap<>();
        qryParams.put(CAT3.REQUEST, CAT3.GET_RECORDS);
        qryParams.put(CAT3.SERVICE, CAT3.SERVICE_TYPE_CODE);
        qryParams.put(CAT3.VERSION, CAT3.VERSION_3_0_0);
        qryParams.put(CAT3.TYPE_NAMES, "Record");
        qryParams.put(CAT3.ELEMENT_SET, "undefined-view");
        request = ClientUtils.buildGetRequest(this.getURI, qryParams,
                MediaType.APPLICATION_XML_TYPE);
        response = this.client.handle(request);
        ETSAssert.assertExceptionReport(response, CAT3.INVALID_PARAM_VAL,
                CAT3.ELEMENT_SET);
    }

    /**
     * [Test] Submits a GetRecords request that specifies an unsupported output
     * schema ("urn:uuid:6a29d2a8-9651-47a6-9b14-f05d2b5644f0"). An exception
     * report is expected in response with HTTP status code 400 and exception
     * code "{@value org.opengis.cite.cat30.CAT3#INVALID_PARAM_VAL}".
     */
    @Test(description = "Requirements: 035,037,042")
    public void getRecordsWithUnsupportedSchema() {
        Map<String, String> qryParams = new HashMap<>();
        qryParams.put(CAT3.REQUEST, CAT3.GET_RECORDS);
        qryParams.put(CAT3.SERVICE, CAT3.SERVICE_TYPE_CODE);
        qryParams.put(CAT3.VERSION, CAT3.VERSION_3_0_0);
        qryParams.put(CAT3.TYPE_NAMES, "Record");
        qryParams.put(CAT3.ELEMENT_SET, CAT3.ELEMENT_SET_BRIEF);
        qryParams.put(CAT3.OUTPUT_SCHEMA, "urn:uuid:6a29d2a8-9651-47a6-9b14-f05d2b5644f0");
        request = ClientUtils.buildGetRequest(this.getURI, qryParams,
                MediaType.APPLICATION_XML_TYPE);
        response = this.client.handle(request);
        ETSAssert.assertExceptionReport(response, CAT3.INVALID_PARAM_VAL,
                CAT3.OUTPUT_SCHEMA);
    }

    /**
     * [Test] Submits a GetRecords request where the 'elementName' parameter
     * value identifies a single element from the output schema (dc:title). The
     * response must be augmented with additional elements so as to be schema
     * valid. Furthermore, every record in the result set must contain one or
     * more (required) dc:title elements; no other optional elements must appear.
     */
    @Test(description = "Requirements: 093")
    public void presentTitleProperty() {
        Map<String, String> qryParams = new HashMap<>();
        qryParams.put(CAT3.REQUEST, CAT3.GET_RECORDS);
        qryParams.put(CAT3.SERVICE, CAT3.SERVICE_TYPE_CODE);
        qryParams.put(CAT3.VERSION, CAT3.VERSION_3_0_0);
        qryParams.put(CAT3.TYPE_NAMES, "Record");
        qryParams.put(CAT3.ELEMENT_NAME, "tns:title");
        qryParams.put(CAT3.NAMESPACE,
                String.format("xmlns(tns=%s)", Namespaces.DCMES));
        request = ClientUtils.buildGetRequest(this.getURI, qryParams,
                MediaType.APPLICATION_XML_TYPE);
        response = this.client.handle(request);
        Assert.assertEquals(response.getStatus(),
                ClientResponse.Status.OK.getStatusCode(),
                ErrorMessage.get(ErrorMessageKeys.UNEXPECTED_STATUS));
        Document entity = getResponseEntityAsDocument(response, null);
        Validator validator = this.cswSchema.newValidator();
        ETSAssert.assertSchemaValid(validator,
                new DOMSource(entity, entity.getDocumentURI()));
        Element results = (Element) entity.getElementsByTagNameNS(
                Namespaces.CSW, CAT3.SEARCH_RESULTS).item(0);
        Map<String, String> namespaceBindings = new HashMap<>();
        namespaceBindings.put(Namespaces.DCMES,"dc");
        namespaceBindings.put(Namespaces.CSW,"csw");
        namespaceBindings.put(Namespaces.OWS,"ows");
        ETSAssert.assertXPath(
                "not(csw:SummaryRecord[dc:type or subject or dc:format or ows:BoundingBox])",
                results, namespaceBindings);
    }

    /**
     * [Test] Submits a GetRecords request that specifies an element name not
     * declared in the output schema. An exception report is expected in
     * response with HTTP status code 400 and exception code
     * "{@value org.opengis.cite.cat30.CAT3#INVALID_PARAM_VAL}".
     */
    @Test(description = "Requirements: 091")
    public void presentUnknownRecordProperty() {
        Map<String, String> qryParams = new HashMap<>();
        qryParams.put(CAT3.REQUEST, CAT3.GET_RECORDS);
        qryParams.put(CAT3.SERVICE, CAT3.SERVICE_TYPE_CODE);
        qryParams.put(CAT3.VERSION, CAT3.VERSION_3_0_0);
        qryParams.put(CAT3.TYPE_NAMES, "Record");
        qryParams.put(CAT3.ELEMENT_NAME, "undefined");
        request = ClientUtils.buildGetRequest(this.getURI, qryParams,
                MediaType.APPLICATION_XML_TYPE);
        response = this.client.handle(request);
        ETSAssert.assertExceptionReport(response, CAT3.INVALID_PARAM_VAL,
                CAT3.ELEMENT_NAME);
    }

    /**
     * [Test] Submits a GetRecords request that contains both the
     * <code>ElementName</code> and <code>ElementSetName</code> parameters.
     * Since these parameters are mutually exclusive, an exception report is
     * expected in response with HTTP status code 400 and exception code
     * "{@value org.opengis.cite.cat30.CAT3#NO_CODE}".
     */
    @Test(description = "Requirements: 099")
    public void elementSetAndElementName() {
        Map<String, String> qryParams = new HashMap<>();
        qryParams.put(CAT3.REQUEST, CAT3.GET_RECORDS);
        qryParams.put(CAT3.SERVICE, CAT3.SERVICE_TYPE_CODE);
        qryParams.put(CAT3.VERSION, CAT3.VERSION_3_0_0);
        qryParams.put(CAT3.TYPE_NAMES, "Record");
        qryParams.put(CAT3.ELEMENT_SET, CAT3.ELEMENT_SET_BRIEF);
        qryParams.put(CAT3.ELEMENT_NAME, "ns1:subject");
        qryParams.put(CAT3.NAMESPACE, String.format("xmlns(ns1=%s)", Namespaces.DCMES));
        request = ClientUtils.buildGetRequest(this.getURI, qryParams,
                MediaType.APPLICATION_XML_TYPE);
        response = this.client.handle(request);
        ETSAssert.assertExceptionReport(response, CAT3.NO_CODE, null);
    }

    /**
     * [Test] Submits a GetRecords request where the <code>startPosition</code>
     * and <code>maxRecords</code> parameters have non-default values. The
     * csw:SearchResults element in the response entity must present the correct
     * "slice" of the result set.
     */
    @Test(description = "Requirements: 082,084")
    public void getPartialResults() {
        Map<String, String> qryParams = new HashMap<>();
        qryParams.put(CAT3.REQUEST, CAT3.GET_RECORDS);
        qryParams.put(CAT3.SERVICE, CAT3.SERVICE_TYPE_CODE);
        qryParams.put(CAT3.VERSION, CAT3.VERSION_3_0_0);
        qryParams.put(CAT3.NAMESPACE, String.format("xmlns(csw3=%s)", Namespaces.CSW));
        qryParams.put(CAT3.TYPE_NAMES, "csw3:Record");
        qryParams.put(CAT3.ELEMENT_SET, CAT3.ELEMENT_SET_SUMMARY);
        int startPosition = 3;
        qryParams.put(CAT3.START_POS, Integer.toString(startPosition));
        int maxRecords = 2;
        qryParams.put(CAT3.MAX_RECORDS, Integer.toString(maxRecords));
        request = ClientUtils.buildGetRequest(this.getURI, qryParams,
                MediaType.APPLICATION_XML_TYPE);
        response = this.client.handle(request);
        Assert.assertEquals(response.getStatus(),
                ClientResponse.Status.OK.getStatusCode(),
                ErrorMessage.get(ErrorMessageKeys.UNEXPECTED_STATUS));
        Document entity = getResponseEntityAsDocument(response, null);
        Node resultsNode = entity.getElementsByTagNameNS(
                Namespaces.CSW, "SearchResults").item(0);
        Assert.assertNotNull(resultsNode,
                ErrorMessage.format(ErrorMessageKeys.MISSING_INFOSET_ITEM, "csw:SearchResults"));
        Element results = (Element) resultsNode;
        Assert.assertEquals(results.getElementsByTagNameNS(Namespaces.CSW, "SummaryRecord").getLength(),
                maxRecords,
                ErrorMessage.format(ErrorMessageKeys.RESULT_SET_SIZE, "csw:SummaryRecord"));
        Assert.assertEquals(Integer.parseInt(results.getAttribute(CAT3.NUM_REC_RETURNED)),
                maxRecords,
                ErrorMessage.format(ErrorMessageKeys.INFOSET_ITEM_VALUE, "@numberOfRecordsReturned"));
        Assert.assertEquals(Integer.parseInt(results.getAttribute(CAT3.NEXT_REC)),
                startPosition + maxRecords,
                ErrorMessage.format(ErrorMessageKeys.INFOSET_ITEM_VALUE, "@nextRecord"));
    }

    /**
     * [Test] Submits a GetRecords request where the <code>startPosition</code>
     * and <code>maxRecords</code> parameters have non-default values. The
     * atom:feed element in the response entity must present the correct "slice"
     * of the result set.
     *
     * @see "OGC Catalogue Services 3.0 Specification - HTTP Protocol Binding,
     * 7.3.6: Atom response"
     * @see "OGC OpenSearch Geo and Time Extensions, Table 6"
     */
    @Test(description = "Requirements: 082,084,122")
    public void getPartialResultsAsAtomFeed() {
        Map<String, String> qryParams = new HashMap<>();
        qryParams.put(CAT3.REQUEST, CAT3.GET_RECORDS);
        qryParams.put(CAT3.SERVICE, CAT3.SERVICE_TYPE_CODE);
        qryParams.put(CAT3.VERSION, CAT3.VERSION_3_0_0);
        qryParams.put(CAT3.NAMESPACE, String.format("xmlns(csw3=%s)", Namespaces.CSW));
        qryParams.put(CAT3.TYPE_NAMES, "csw3:Record");
        qryParams.put(CAT3.ELEMENT_SET, CAT3.ELEMENT_SET_SUMMARY);
        qryParams.put(CAT3.OUTPUT_FORMAT, MediaType.APPLICATION_ATOM_XML);
        int startPosition = 3;
        qryParams.put(CAT3.START_POS, Integer.toString(startPosition));
        int maxRecords = 2;
        qryParams.put(CAT3.MAX_RECORDS, Integer.toString(maxRecords));
        request = ClientUtils.buildGetRequest(this.getURI, qryParams,
                MediaType.APPLICATION_ATOM_XML_TYPE);
        response = this.client.handle(request);
        Assert.assertEquals(response.getStatus(),
                ClientResponse.Status.OK.getStatusCode(),
                ErrorMessage.get(ErrorMessageKeys.UNEXPECTED_STATUS));
        Document entity = getResponseEntityAsDocument(response, null);
        Node feedNode = entity.getElementsByTagNameNS(Namespaces.ATOM, "feed").item(0);
        Assert.assertNotNull(feedNode,
                ErrorMessage.format(ErrorMessageKeys.MISSING_INFOSET_ITEM, "atom:feed"));
        Element feed = (Element) feedNode;
        Assert.assertEquals(feed.getElementsByTagNameNS(Namespaces.ATOM, "entry").getLength(),
                maxRecords,
                ErrorMessage.format(ErrorMessageKeys.RESULT_SET_SIZE, "atom:entry"));
        Node startIndex = feed.getElementsByTagNameNS(Namespaces.OSD11, "startIndex").item(0);
        Assert.assertNotNull(startIndex,
                ErrorMessage.format(ErrorMessageKeys.MISSING_INFOSET_ITEM, "os:startIndex"));
        Assert.assertEquals(Integer.parseInt(startIndex.getTextContent()),
                startPosition,
                ErrorMessage.format(ErrorMessageKeys.INFOSET_ITEM_VALUE, "os:startIndex"));
        Node itemsPerPage = feed.getElementsByTagNameNS(Namespaces.OSD11, "itemsPerPage").item(0);
        Assert.assertNotNull(itemsPerPage,
                ErrorMessage.format(ErrorMessageKeys.MISSING_INFOSET_ITEM, "os:itemsPerPage"));
        Assert.assertEquals(Integer.parseInt(itemsPerPage.getTextContent()),
                maxRecords,
                ErrorMessage.format(ErrorMessageKeys.INFOSET_ITEM_VALUE, "os:itemsPerPage"));
    }
}
