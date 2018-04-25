package org.opengis.cite.cat30.basic;

import com.sun.jersey.api.client.ClientResponse;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
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
import org.xml.sax.SAXException;

/**
 * Provides tests that apply to the <code>GetRecordById</code> request. This
 * request implements the abstract <em>GetResourceByID</em> operation defined in
 * the OGCWebService interface (OGC 06-121r9, Figure C.2).
 * <p>
 * <p>
 * The KVP syntax must be supported; this encoding is generally used with the
 * GET method but may also be used with the POST method; this latter capability
 * will be advertised in the capabilities document as an operational constraint
 * as indicated below. The media type of a KVP request entity is
 * "application/x-www-form-urlencoded".
 * </p>
 * <p>
 * <pre>{@literal
 * <Post xmlns="http://www.opengis.net/ows/2.0"
 *  xmlns:xlink="http://www.w3.org/1999/xlink"
 *  xlink:href="http://cat.example.org/csw">
 *  <Constraint name="PostEncoding">
 *    <AllowedValues>
 *      <Value>KVP</Value>
 *    </AllowedValues>
 *  </Constraint>
 * </Post>
 * }
 * </pre>
 * <p>
 * <p style="margin-bottom: 0.5em"><strong>Sources</strong></p>
 * <ul>
 * <li>OGC 12-176r6, 7.4: GetRecordById operation</li>
 * <li>OGC 12-176r6, Table 16: Operation constraints</li>
 * <li>OGC 06-121r9, 7.4.7: OperationsMetadata section standard contents</li>
 * </ul>
 */
public class GetRecordByIdTests extends CommonFixture {

    /**
     * Service endpoint for GetRecordById using the GET method.
     */
    private URI getURI;
    /**
     * Service endpoint for GetRecordById using the POST method.
     */
    private URI postURI;
    /**
     * A list of record identifiers retrieved from the SUT.
     */
    private List<String> idList;

    void setIdList(List<String> idList) {
        this.idList = idList;
    }

    /**
     * Finds the GET and POST method endpoints for the GetCapabilities request
     * in the capabilities document.
     */
    @BeforeClass
    public void findRequestEndpoints() {
        this.getURI = ServiceMetadataUtils.getOperationEndpoint(
                this.cswCapabilities, CAT3.GET_RECORD_BY_ID, HttpMethod.GET);
        this.postURI = ServiceMetadataUtils.getOperationEndpoint(
                this.cswCapabilities, CAT3.GET_RECORD_BY_ID, HttpMethod.POST);
    }

    /**
     * Gets the record identifiers that occur in the sample data obtained from
     * the SUT. Each csw:Record element must contain at least one dc:identifier
     * element.
     *
     * @param testContext The test context containing various suite attributes.
     */
    @BeforeClass
    public void getRecordIdentifiers(ITestContext testContext) {
        DatasetInfo dataset = (DatasetInfo) testContext.getSuite().getAttribute(
                SuiteAttribute.DATASET.getName());
        if (null == dataset) {
            throw new SkipException("Dataset info not found in test context.");
        }
        List<String> identifiers = dataset.getRecordIdentifiers();
        if (identifiers.isEmpty()) {
            throw new SkipException("No dc:identifier elements found in sample data.");
        }
        setIdList(identifiers);
    }

    /**
     * [Test] Verifies that a request for a record by identifier produces a
     * response with status code 404 (Not Found) if no matching resource
     * representation is found. A response entity (an exception report) is
     * optional; if present, the exception code shall be
     * "InvalidParameterValue".
     *
     * @see "OGC 12-176r6, 7.4.4.2: 7.4.4.2	Id parameter"
     */
    @Test(description = "Requirements: 127,141")
    public void getRecordById_noMatch() {
        Map<String, String> qryParams = new HashMap<>();
        qryParams.put(CAT3.REQUEST, CAT3.GET_RECORD_BY_ID);
        qryParams.put(CAT3.SERVICE, CAT3.SERVICE_TYPE_CODE);
        qryParams.put(CAT3.VERSION, CAT3.VERSION_3_0_0);
        qryParams.put(CAT3.ID, "urn:example:" + System.currentTimeMillis());
        request = ClientUtils.buildGetRequest(this.getURI,
                qryParams, MediaType.APPLICATION_XML_TYPE);
        response = this.client.handle(request);
        Assert.assertEquals(response.getStatus(),
                ClientResponse.Status.NOT_FOUND.getStatusCode(),
                ErrorMessage.get(ErrorMessageKeys.UNEXPECTED_STATUS));
    }

    /**
     * [Test] Verifies that a request for a record by identifier produces a
     * matching csw:SummaryRecord in the response entity. The default view
     * (element set) is "summary". The default output schema is identified by
     * the namespace name {@value org.opengis.cite.cat30.Namespaces#CSW}.
     *
     * @see "OGC 12-176r6, 7.4.4.2: Id parameter"
     * @see "OGC 12-176r6, 7.4.5: Response"
     */
    @Test(description = "Requirements: 124,134")
    public void getSummaryRecordById() {
        Map<String, String> qryParams = new HashMap<>();
        qryParams.put(CAT3.REQUEST, CAT3.GET_RECORD_BY_ID);
        qryParams.put(CAT3.SERVICE, CAT3.SERVICE_TYPE_CODE);
        qryParams.put(CAT3.VERSION, CAT3.VERSION_3_0_0);
        int randomIndex = ThreadLocalRandom.current().nextInt(this.idList.size());
        String id = this.idList.get(randomIndex);
        qryParams.put(CAT3.ID, id);
        request = ClientUtils.buildGetRequest(this.getURI,
                qryParams, MediaType.APPLICATION_XML_TYPE);
        response = this.client.handle(request);
        Assert.assertEquals(response.getStatus(),
                ClientResponse.Status.OK.getStatusCode(),
                ErrorMessage.get(ErrorMessageKeys.UNEXPECTED_STATUS));
        Document entity = ClientUtils.getResponseEntityAsDocument(response, null);
        String expr = String.format("/csw:SummaryRecord/dc:identifier = '%s'",
                id);
        Map<String, String> namespaceBindings = new HashMap<>();
        namespaceBindings.put(Namespaces.DCMES, "dc");
        namespaceBindings.put(Namespaces.CSW, "csw");
        namespaceBindings.put(Namespaces.OWS, "ows");
        ETSAssert.assertXPath(expr, entity, namespaceBindings);
    }

    /**
     * [Test] Verifies that a request for a brief record by identifier produces
     * a matching csw:BriefRecord in the response entity. The entity must be
     * schema-valid.
     *
     * @see "OGC 12-176r6, 7.4.4.1: ElementSetName parameter"
     */
    @Test(description = "Requirements: 123")
    public void getBriefRecordById() {
        Map<String, String> qryParams = new HashMap<>();
        qryParams.put(CAT3.REQUEST, CAT3.GET_RECORD_BY_ID);
        qryParams.put(CAT3.SERVICE, CAT3.SERVICE_TYPE_CODE);
        qryParams.put(CAT3.VERSION, CAT3.VERSION_3_0_0);
        qryParams.put(CAT3.ELEMENT_SET, CAT3.ELEMENT_SET_BRIEF);
        int randomIndex = ThreadLocalRandom.current().nextInt(this.idList.size());
        String id = this.idList.get(randomIndex);
        qryParams.put(CAT3.ID, id);
        request = ClientUtils.buildGetRequest(this.getURI,
                qryParams, MediaType.APPLICATION_XML_TYPE);
        response = this.client.handle(request);
        Assert.assertEquals(response.getStatus(),
                ClientResponse.Status.OK.getStatusCode(),
                ErrorMessage.get(ErrorMessageKeys.UNEXPECTED_STATUS));
        Document entity = ClientUtils.getResponseEntityAsDocument(response, null);
        String expr = String.format("/csw:BriefRecord/dc:identifier = '%s'",
                id);
        Map<String, String> namespaceBindings = new HashMap<>();
        namespaceBindings.put(Namespaces.DCMES, "dc");
        namespaceBindings.put(Namespaces.CSW, "csw");
        namespaceBindings.put(Namespaces.OWS, "ows");
        ETSAssert.assertXPath(expr, entity, namespaceBindings);
        Validator validator = this.cswSchema.newValidator();
        ETSAssert.assertSchemaValid(validator, new DOMSource(entity));
    }

    /**
     * [Test] Verifies that a request for a full record by identifier produces a
     * matching csw:Record in the response entity. The entity must be
     * schema-valid.
     *
     * @see "OGC 12-176r6, 7.4.4.1: ElementSetName parameter"
     */
    @Test(description = "Requirements: 123")
    public void getFullRecordById() {
        Map<String, String> qryParams = new HashMap<>();
        qryParams.put(CAT3.REQUEST, CAT3.GET_RECORD_BY_ID);
        qryParams.put(CAT3.SERVICE, CAT3.SERVICE_TYPE_CODE);
        qryParams.put(CAT3.VERSION, CAT3.VERSION_3_0_0);
        qryParams.put(CAT3.ELEMENT_SET, CAT3.ELEMENT_SET_FULL);
        int randomIndex = ThreadLocalRandom.current().nextInt(this.idList.size());
        String id = this.idList.get(randomIndex);
        qryParams.put(CAT3.ID, id);
        request = ClientUtils.buildGetRequest(this.getURI,
                qryParams, MediaType.APPLICATION_XML_TYPE);
        response = this.client.handle(request);
        Assert.assertEquals(response.getStatus(),
                ClientResponse.Status.OK.getStatusCode(),
                ErrorMessage.get(ErrorMessageKeys.UNEXPECTED_STATUS));
        Document entity = ClientUtils.getResponseEntityAsDocument(response, null);
        String expr = String.format("/csw:Record/dc:identifier = '%s'",
                id);
        Map<String, String> namespaceBindings = new HashMap<>();
        namespaceBindings.put(Namespaces.DCMES, "dc");
        namespaceBindings.put(Namespaces.CSW, "csw");
        namespaceBindings.put(Namespaces.OWS, "ows");
        ETSAssert.assertXPath(expr, entity, namespaceBindings);
        Validator validator = this.cswSchema.newValidator();
        ETSAssert.assertSchemaValid(validator, new DOMSource(entity));
    }

    /**
     * [Test] Verifies that a request for an Atom representation of a record
     * produces a matching atom:entry in the response entity. The
     * <code>Accept</code> request header indicates a preference for Atom
     * content; the outputFormat parameter is omitted (thus the header value
     * applies). The content of the entry must conform to RFC 4287.
     * <p>
     * <p>
     * The atom:entry element is expected to include a dc:identifier element in
     * accord with the mappings given in OGC 10-032r8, Table 7.
     * </p>
     * <p>
     * <pre>{@literal
     * <entry xmlns="http://www.w3.org/2005/Atom"
     *  xmlns:dc="http://purl.org/dc/elements/1.1/">
     *  <id>http://csw.example.org/record/ff711198-b30f-11e4-a71e-12e3f512a338</id>
     *  <title>Title</title>
     *  <updated>2015-02-12T23:46:57Z</updated>
     *  <dc:identifier>ff711198-b30f-11e4-a71e-12e3f512a338</dc:identifier>
     * </entry>
     * }
     * </pre>
     * <p>
     * <p style="margin-bottom: 0.5em"><strong>Sources</strong></p>
     * <ul>
     * <li>OGC 12-176r6, 7.4.4.4: outputSchema parameter</li>
     * <li>OGC 10-032r8, 9.3.2: Normal response XML encoding</li>
     * <li><a href="https://tools.ietf.org/html/rfc4287" target="_blank">RFC
     * 4287</a>: The Atom Syndication Format</li>
     * </ul>
     */
    @Test(description = "Requirements: 003,139,140")
    public void getRecordByIdAsAtomEntryUsingAcceptHeader() {
        Map<String, String> qryParams = new HashMap<>();
        qryParams.put(CAT3.REQUEST, CAT3.GET_RECORD_BY_ID);
        qryParams.put(CAT3.SERVICE, CAT3.SERVICE_TYPE_CODE);
        qryParams.put(CAT3.VERSION, CAT3.VERSION_3_0_0);
        int randomIndex = ThreadLocalRandom.current().nextInt(this.idList.size());
        String id = this.idList.get(randomIndex);
        qryParams.put(CAT3.ID, id);
        request = buildGetRequest(this.getURI, qryParams,
                MediaType.APPLICATION_ATOM_XML_TYPE);
        response = this.client.handle(request);
        Assert.assertEquals(response.getStatus(),
                ClientResponse.Status.OK.getStatusCode(),
                ErrorMessage.get(ErrorMessageKeys.UNEXPECTED_STATUS));
        Document entity = getResponseEntityAsDocument(response, null);
        Map<String, String> nsBindings = new HashMap<>();
        nsBindings.put(Namespaces.ATOM, "atom");
        nsBindings.put(Namespaces.DCMES, "dc");
        nsBindings.put(Namespaces.CSW, "csw");
        nsBindings.put(Namespaces.OWS, "ows");
        String expr = String.format("/atom:entry/dc:identifier = '%s'",
                id);
        ETSAssert.assertXPath(expr, entity, nsBindings);
        Validator atomValidator = this.atomSchema.newValidator();
        ValidationErrorHandler err = new ValidationErrorHandler();
        atomValidator.setErrorHandler(err);
        try {
            // Jing Validator implementation rejects DOMSource as input
            Source src = XMLUtils.toStreamSource(new DOMSource(entity));
            atomValidator.validate(src);
        } catch (SAXException | IOException ex) {
            Logger.getLogger(GetRecordByIdTests.class.getName()).log(
                    Level.WARNING, "Error attempting to validate Atom entry.", ex);
        }
        Assert.assertFalse(err.errorsDetected(),
                ErrorMessage.format(ErrorMessageKeys.NOT_SCHEMA_VALID,
                        err.getErrorCount(), err.toString()));
    }

    /**
     * [Test] Verifies that a request for an Atom representation of a record
     * produces a matching atom:entry in the response entity. The outputFormat
     * query parameter value ("application/atom+xml") overrides the Accept
     * request header ("application/xml").
     */
    @Test(description = "Requirements: 002,135,140")
    public void getRecordByIdAsAtomEntryUsingOutputFormat() {
        Map<String, String> qryParams = new HashMap<>();
        qryParams.put(CAT3.REQUEST, CAT3.GET_RECORD_BY_ID);
        qryParams.put(CAT3.SERVICE, CAT3.SERVICE_TYPE_CODE);
        qryParams.put(CAT3.VERSION, CAT3.VERSION_3_0_0);
        qryParams.put(CAT3.OUTPUT_FORMAT, MediaType.APPLICATION_ATOM_XML);
        int randomIndex = ThreadLocalRandom.current().nextInt(this.idList.size());
        String id = this.idList.get(randomIndex);
        qryParams.put(CAT3.ID, id);
        request = ClientUtils.buildGetRequest(this.getURI, qryParams,
                MediaType.APPLICATION_XML_TYPE);
        response = this.client.handle(request);
        Assert.assertEquals(response.getStatus(),
                ClientResponse.Status.OK.getStatusCode(),
                ErrorMessage.get(ErrorMessageKeys.UNEXPECTED_STATUS));
        Document entity = ClientUtils.getResponseEntityAsDocument(response, null);
        Map<String, String> nsBindings = new HashMap<>();
        nsBindings.put(Namespaces.ATOM, "atom");
        nsBindings.put(Namespaces.DCMES, "dc");
        nsBindings.put(Namespaces.CSW, "csw");
        nsBindings.put(Namespaces.OWS, "ows");
        String expr = String.format("/atom:entry/dc:identifier = '%s'",
                id);
        ETSAssert.assertXPath(expr, entity, nsBindings);
    }

    /**
     * [Test] Verifies that a request for a record representation in an
     * unsupported format (media type) produces an exception report containing
     * the exception code "InvalidParameterValue". The
     * {@value org.opengis.cite.cat30.CAT3#OUTPUT_FORMAT} parameter has the
     * value "model/vnd.collada+xml".
     *
     * @see "OGC 12-176r6, 7.4.4.3: outputFormat parameter"
     */
    @Test(description = "Requirements: 002,035,128")
    public void getRecordByIdWithUnsupportedFormat() {
        Map<String, String> qryParams = new HashMap<>();
        qryParams.put(CAT3.REQUEST, CAT3.GET_RECORD_BY_ID);
        qryParams.put(CAT3.SERVICE, CAT3.SERVICE_TYPE_CODE);
        qryParams.put(CAT3.VERSION, CAT3.VERSION_3_0_0);
        qryParams.put(CAT3.OUTPUT_FORMAT, "model/vnd.collada+xml");
        int randomIndex = ThreadLocalRandom.current().nextInt(this.idList.size());
        String id = this.idList.get(randomIndex);
        qryParams.put(CAT3.ID, id);
        request = ClientUtils.buildGetRequest(this.getURI,
                qryParams, MediaType.APPLICATION_XML_TYPE);
        response = this.client.handle(request);
        ETSAssert.assertExceptionReport(response, CAT3.INVALID_PARAM_VAL,
                CAT3.OUTPUT_FORMAT);
    }

    /**
     * [Test] Verifies that a request for a record that conforms to an
     * unsupported output schema produces an exception report containing the
     * exception code "InvalidParameterValue". The
     * {@value org.opengis.cite.cat30.CAT3#OUTPUT_SCHEMA} parameter has the
     * value "http://www.example.org/ns/alpha".
     *
     * @see "OGC 12-176r6, 7.4.4.4: outputSchema parameter"
     */
    @Test(description = "Requirements: 132,136")
    public void getRecordByIdWithUnsupportedSchema() {
        Map<String, String> qryParams = new HashMap<>();
        qryParams.put(CAT3.REQUEST, CAT3.GET_RECORD_BY_ID);
        qryParams.put(CAT3.SERVICE, CAT3.SERVICE_TYPE_CODE);
        qryParams.put(CAT3.VERSION, CAT3.VERSION_3_0_0);
        qryParams.put(CAT3.OUTPUT_SCHEMA, "http://www.example.org/ns/alpha");
        int randomIndex = ThreadLocalRandom.current().nextInt(this.idList.size());
        String id = this.idList.get(randomIndex);
        qryParams.put(CAT3.ID, id);
        request = ClientUtils.buildGetRequest(this.getURI,
                qryParams, MediaType.APPLICATION_XML_TYPE);
        response = this.client.handle(request);
        ETSAssert.assertExceptionReport(response, CAT3.INVALID_PARAM_VAL,
                CAT3.OUTPUT_SCHEMA);
    }

    /**
     * [Test] Verifies that a request for a record that omits the required 'id'
     * parameter produces an exception report containing the exception code
     * "MissingParameterValue".
     *
     * @see "OGC 12-176r6, Table 21: KVP encoding for GetRecordById operation
     * request"
     */
    @Test(description = "Requirements: 037")
    public void getRecordByIdWithMissingId() {
        Map<String, String> qryParams = new HashMap<>();
        qryParams.put(CAT3.REQUEST, CAT3.GET_RECORD_BY_ID);
        qryParams.put(CAT3.SERVICE, CAT3.SERVICE_TYPE_CODE);
        qryParams.put(CAT3.VERSION, CAT3.VERSION_3_0_0);
        request = ClientUtils.buildGetRequest(this.getURI,
                qryParams, MediaType.APPLICATION_XML_TYPE);
        response = this.client.handle(request);
        ETSAssert.assertExceptionReport(response, CAT3.MISSING_PARAM_VAL, CAT3.ID);
    }
}
