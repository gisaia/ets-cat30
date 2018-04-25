package org.opengis.cite.cat30.basic;

import com.sun.jersey.api.client.ClientResponse;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmValue;
import org.geotoolkit.geometry.Envelopes;
import org.geotoolkit.geometry.GeneralEnvelope;
import org.geotoolkit.referencing.crs.DefaultGeographicCRS;
import org.opengis.cite.cat30.CAT3;
import org.opengis.cite.cat30.CommonFixture;
import org.opengis.cite.cat30.ETSAssert;
import org.opengis.cite.cat30.ErrorMessage;
import org.opengis.cite.cat30.ErrorMessageKeys;
import org.opengis.cite.cat30.Namespaces;
import org.opengis.cite.cat30.SuiteAttribute;
import org.opengis.cite.cat30.util.ClientUtils;
import org.opengis.cite.cat30.util.DatasetInfo;
import org.opengis.cite.cat30.util.Records;
import org.opengis.cite.cat30.util.ServiceMetadataUtils;
import org.opengis.cite.cat30.util.URIUtils;
import org.opengis.cite.cat30.util.XMLUtils;
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
import org.w3c.dom.NodeList;

/**
 * Includes GetRecords tests pertaining to the <code>Filter-FES-KVP</code>
 * conformance class. The following basic search capabilities must be
 * implemented:
 * <ul>
 * <li>Text search using the 'q' query parameter;</li>
 * <li>Record search using the 'recordIds' query parameter;</li>
 * <li>Spatial search using the 'bbox' query parameter.</li>
 * </ul>
 *
 * @see "OGC 12-176r6, Table 1: Conformance classes"
 * @see "OGC 12-176r6, Table 6: KVP encoding for query constraints"
 */
public class BasicSearchTests extends CommonFixture {

	/**
	 * Service endpoint for GetRecords using the GET method.
	 */
	private URI getURI;
	/**
	 * An Envelope defining the total geographic extent of the sample data.
	 */
	private Envelope geoExtent;
	/**
	 * Information about the sample data retrieved from the IUT.
	 */
	private DatasetInfo datasetInfo;
	/**
	 * A list of record titles retrieved from the IUT.
	 */
	private List<String> recordTitles;
	/**
	 * A list of record identifiers retrieved from the IUT.
	 */
	private List<String> recordIdentifiers;
	/**
	 * A list of record topics retrieved from the IUT.
	 */
	private List<String> recordTopics;

	void setExtent(Envelope extent) {
		this.geoExtent = extent;
	}

	void setGetEndpoint(URI uri) {
		this.getURI = uri;
	}

	/**
	 * Finds the GET method endpoint for the GetRecords request in the
	 * capabilities document.
	 *
	 * @param testContext
	 *            The test context containing various suite attributes.
	 */
	@BeforeClass
	public void findRequestEndpoints(ITestContext testContext) {
		this.getURI = ServiceMetadataUtils.getOperationEndpoint(
				this.cswCapabilities, CAT3.GET_RECORDS, HttpMethod.GET);
		if (null == this.getURI.getScheme()) {
			throw new SkipException(
					"GET endpoint for GetRecords request not found.");
		}
	}

	/**
	 * Gets information about the sample data obtained from the IUT, including:
	 * its total geographic extent; a collection of record titles; and a set of
	 * record identifiers. Each csw:Record element may contain at least one
	 * ows:BoundingBox (or ows:WGS84BoundingBox) element that describes the
	 * spatial coverage of a catalogued resource.
	 *
	 * @param testContext
	 *            The test context containing various suite attributes.
	 */
	@BeforeClass
	public void getDatasetInfo(ITestContext testContext) {
		DatasetInfo dataset = (DatasetInfo) testContext.getSuite()
				.getAttribute(SuiteAttribute.DATASET.getName());
		if (null == dataset) {
			throw new SkipException("Dataset info not found in test context.");
		}
		this.datasetInfo = dataset;
		Envelope env = datasetInfo.getGeographicExtent();
		setExtent(env);
		this.recordTitles = datasetInfo.getRecordTitles();
		this.recordIdentifiers = datasetInfo.getRecordIdentifiers();
		this.recordTopics = datasetInfo.getRecordTopics();
	}

	/**
	 * [Test] Submits a GetRecords request with a BBOX parameter that includes
	 * an invalid CRS reference ("urn:ogc:def:crs:EPSG::0000"). An exception
	 * report is expected in response with HTTP status code 400 and exception
	 * code "{@value org.opengis.cite.cat30.CAT3#INVALID_PARAM_VAL}".
	 *
	 * <p style="margin-bottom: 0.5em">
	 * <strong>Sources</strong>
	 * </p>
	 * <ul>
	 * <li>OGC 12-176r6, Table 6: KVP encoding for query constraints</li>
	 * <li>OGC 06-121r9, 10.2.3: Bounding box KVP encoding</li>
	 * </ul>
	 */
	@Test(description = "Requirements: 017")
	public void getRecordsByBBOXWithUnsupportedCRS() {
		Map<String, String> qryParams = new HashMap<>();
		qryParams.put(CAT3.REQUEST, CAT3.GET_RECORDS);
		qryParams.put(CAT3.SERVICE, CAT3.SERVICE_TYPE_CODE);
		qryParams.put(CAT3.VERSION, CAT3.VERSION_3_0_0);
		qryParams.put(CAT3.TYPE_NAMES, "Record");
		qryParams.put(CAT3.ELEMENT_SET, CAT3.ELEMENT_SET_BRIEF);
		qryParams.put(CAT3.BBOX,
				"472944,5363287,492722,5455253,urn:ogc:def:crs:EPSG::0000");
		request = ClientUtils.buildGetRequest(this.getURI, qryParams,
				MediaType.APPLICATION_XML_TYPE);
		response = this.client.handle(request);
		ETSAssert.assertExceptionReport(response, CAT3.INVALID_PARAM_VAL,
				CAT3.BBOX);
	}

	/**
	 * [Test] Submits a GetRecords request with a BBOX parameter that includes a
	 * supported CRS reference. The brief records in the response must all
	 * contain an ows:BoundingBox (or ows:WGS84BoundingBox) element that
	 * intersects the specified bounding box.
	 *
	 * <p style="margin-bottom: 0.5em">
	 * <strong>Sources</strong>
	 * </p>
	 * <ul>
	 * <li>OGC 12-176r6, Table 1: Conformance classes [Filter-FES-KVP]</li>
	 * <li>OGC 12-176r6, Table 6: KVP encoding for query constraints</li>
	 * <li>OGC 06-121r9, 10.2.3: Bounding box KVP encoding</li>
	 * <li>OGC 09-026r2 (ISO 19143), A.7: Test cases for minimum spatial filter</li>
	 * </ul>
	 */
	@Test(description = "Requirements: 017; Tests: 017")
	public void getBriefRecordsByBBOX() {
		if (null == this.geoExtent) {
			throw new SkipException(
					"Could not determine extent of sample data.");
		}
		Map<String, String> qryParams = new HashMap<>();
		qryParams.put(CAT3.REQUEST, CAT3.GET_RECORDS);
		qryParams.put(CAT3.SERVICE, CAT3.SERVICE_TYPE_CODE);
		qryParams.put(CAT3.VERSION, CAT3.VERSION_3_0_0);
		qryParams.put(CAT3.TYPE_NAMES, "Record");
		qryParams.put(CAT3.ELEMENT_SET, CAT3.ELEMENT_SET_BRIEF);
		Envelope bbox = this.geoExtent;
		qryParams.put(CAT3.BBOX, Extents.envelopeToString(bbox));
		request = ClientUtils.buildGetRequest(this.getURI, qryParams,
				MediaType.APPLICATION_XML_TYPE);
		response = this.client.handle(request);
		Assert.assertEquals(response.getStatus(),
				ClientResponse.Status.OK.getStatusCode(),
				ErrorMessage.get(ErrorMessageKeys.UNEXPECTED_STATUS));
		Document entity = getResponseEntityAsDocument(response, null);
		Source results = new DOMSource(entity);
		ETSAssert.assertEnvelopeIntersectsBoundingBoxes(bbox, results);
	}

	/**
	 * [Test] Submits a GetRecords request with a 'bbox' parameter that uses the
	 * default CRS ("urn:ogc:def:crs:OGC:1.3:CRS84"). The summary records in the
	 * response must all contain an ows:BoundingBox (or ows:WGS84BoundingBox)
	 * element that intersects the specified bounding box.
	 *
	 * <p style="margin-bottom: 0.5em">
	 * <strong>Sources</strong>
	 * </p>
	 * <ul>
	 * <li>OGC 12-176r6, Table 1: Conformance classes [Filter-FES-KVP]</li>
	 * <li>OGC 12-176r6, Table 6: KVP encoding for query constraints</li>
	 * <li>OGC 06-121r9, 10.2.3: Bounding box KVP encoding</li>
	 * <li>OGC 09-026r2 (ISO 19143), A.7: Test cases for minimum spatial filter</li>
	 * </ul>
	 */
	@Test(description = "Requirements: 017; Tests: 017")
	public void getSummaryRecordsByWGS84BBOX() {
		if (null == this.geoExtent) {
			throw new SkipException(
					"Could not determine extent of sample data.");
		}
		Map<String, String> qryParams = new HashMap<>();
		qryParams.put(CAT3.REQUEST, CAT3.GET_RECORDS);
		qryParams.put(CAT3.SERVICE, CAT3.SERVICE_TYPE_CODE);
		qryParams.put(CAT3.VERSION, CAT3.VERSION_3_0_0);
		qryParams.put(CAT3.TYPE_NAMES, "Record");
		qryParams.put(CAT3.ELEMENT_SET, CAT3.ELEMENT_SET_SUMMARY);
		Envelope bbox = this.geoExtent;
		try {
			if (!bbox.getCoordinateReferenceSystem().equals(
					DefaultGeographicCRS.WGS84)) {
				bbox = new GeneralEnvelope(Envelopes.transform(bbox,
						DefaultGeographicCRS.WGS84));
			}
		} catch (TransformException ex) {
			throw new RuntimeException("Failed to create WGS84 envelope.", ex);
		}
		qryParams.put(CAT3.BBOX, Extents.envelopeToString(bbox));
		request = ClientUtils.buildGetRequest(this.getURI, qryParams,
				MediaType.APPLICATION_XML_TYPE);
		response = this.client.handle(request);
		Assert.assertEquals(response.getStatus(),
				ClientResponse.Status.OK.getStatusCode(),
				ErrorMessage.get(ErrorMessageKeys.UNEXPECTED_STATUS));
		Document entity = getResponseEntityAsDocument(response, null);
		Source results = new DOMSource(entity);
		ETSAssert.assertEnvelopeIntersectsBoundingBoxes(bbox, results);
	}

	/**
	 * [Test] Submits a GetRecords request with a 'recordIds' parameter that
	 * contains a (comma-separated) list of two record identifiers. Two matching
	 * records (csw:SummaryRecord) are expected in the response.
	 */
	@Test(description = "OGC 12-176, Table 6 - Record search")
	public void getMultipleRecordsById() {
		Map<String, String> qryParams = new HashMap<>();
		qryParams.put(CAT3.REQUEST, CAT3.GET_RECORDS);
		qryParams.put(CAT3.SERVICE, CAT3.SERVICE_TYPE_CODE);
		qryParams.put(CAT3.VERSION, CAT3.VERSION_3_0_0);
		qryParams.put(CAT3.TYPE_NAMES, "Record");
		qryParams.put(CAT3.ELEMENT_SET, CAT3.ELEMENT_SET_SUMMARY);
		List<String> idList = new ArrayList<>();
		// select first and last identifiers
		idList.add(this.recordIdentifiers.get(0));
		StringBuilder paramValue = new StringBuilder();
		paramValue.append(idList.get(0)).append(',');
		idList.add(this.recordIdentifiers.get(this.recordIdentifiers.size() - 1));
		paramValue.append(idList.get(1));
		qryParams.put(CAT3.REC_ID_LIST, paramValue.toString());
		request = ClientUtils.buildGetRequest(this.getURI, qryParams,
				MediaType.APPLICATION_XML_TYPE);
		response = this.client.handle(request);
		Assert.assertEquals(response.getStatus(),
				ClientResponse.Status.OK.getStatusCode(),
				ErrorMessage.get(ErrorMessageKeys.UNEXPECTED_STATUS));
		Document entity = getResponseEntityAsDocument(response, null);
		QName recordName = new QName(Namespaces.CSW, "SummaryRecord");
		NodeList recordList = entity.getElementsByTagNameNS(
				recordName.getNamespaceURI(), recordName.getLocalPart());
		Assert.assertEquals(recordList.getLength(), 2, ErrorMessage.format(
				ErrorMessageKeys.RESULT_SET_SIZE, recordName));
		for (int i = 0; i < recordList.getLength(); i++) {
			Element record = (Element) recordList.item(i);
			NodeList identifiers = record.getElementsByTagNameNS(
					Namespaces.DCMES, "identifier");
			List<String> recIdList = XMLUtils.getNodeValues(identifiers);
			// retain common elements (intersection)
			recIdList.retainAll(idList);
			Assert.assertFalse(
					recIdList.isEmpty(),
					ErrorMessage.format(ErrorMessageKeys.ID_NOT_FOUND,
							Records.getRecordId(record)));
		}
	}

	/**
	 * [Test] Submits a GetRecords request where the 'q' parameter value is a
	 * single URL-encoded term that occurs in at least one catalog record. The
	 * result set must not be empty.
	 *
	 * <p>
	 * <strong>Note: </strong>According to Table 4 in <em>OGC OpenSearch Geo and
	 * Time Extensions</em> (OGC 10-032r8), the domain of keyword matching
	 * should include the record elements indicated below.
	 * </p>
	 *
	 * <table border="1" style="border-collapse: collapse;">
	 * <caption>Recommended scope of keyword matching</caption> <thead>
	 * <tr>
	 * <th>csw:Record</th>
	 * <th>atom:entry</th>
	 * </tr>
	 * </thead> <tbody>
	 * <tr>
	 * <td>
	 * <ul>
	 * <li>dc:title</li>
	 * <li>dc:description (dct:abstract)</li>
	 * <li>dc:subject</li>
	 * </ul>
	 * </td>
	 * <td>
	 * <ul>
	 * <li>atom:title</li>
	 * <li>atom:summary</li>
	 * <li>atom:category</li>
	 * </ul>
	 * </td>
	 * </tr>
	 * </tbody>
	 * </table>
	 *
	 * <p style="margin-bottom: 0.5em">
	 * <strong>Sources</strong>
	 * </p>
	 * <ul>
	 * <li>OGC 12-176r6, Table 1: Conformance classes [Filter-FES-KVP]</li>
	 * <li>OGC 12-176r6, Table 6: KVP encoding for query constraints</li>
	 * <li>OGC 10-032r8, Table 4: Search operation queryable mappings</li>
	 * </ul>
	 */
	@Test(description = "OGC 12-176, Table 6 - Text search")
	public void singleTermTextSearch() {
		Map<String, String> qryParams = new HashMap<>();
		qryParams.put(CAT3.REQUEST, CAT3.GET_RECORDS);
		qryParams.put(CAT3.SERVICE, CAT3.SERVICE_TYPE_CODE);
		qryParams.put(CAT3.VERSION, CAT3.VERSION_3_0_0);
		qryParams.put(CAT3.TYPE_NAMES, "Record");
		qryParams.put(CAT3.ELEMENT_SET, CAT3.ELEMENT_SET_FULL);
		ThreadLocalRandom random = ThreadLocalRandom.current();
		String title = recordTitles.get(random.nextInt(recordTitles.size()));
		while (title.isEmpty()) { // rare but might happen
			title = recordTitles.get(random.nextInt(recordTitles.size()));
		}
		String[] titleWords = title.split("\\s+");
		String keyword = titleWords[titleWords.length - 1];
		// remove any chars that may give rise to invalid XPath expression
		keyword = keyword.replaceAll("[()]", "");
		qryParams.put(CAT3.Q, URIUtils.getPercentEncodedString(keyword));
		request = ClientUtils.buildGetRequest(this.getURI, qryParams,
				MediaType.APPLICATION_XML_TYPE);
		response = this.client.handle(request);
		Assert.assertEquals(response.getStatus(),
				ClientResponse.Status.OK.getStatusCode(),
				ErrorMessage.get(ErrorMessageKeys.UNEXPECTED_STATUS));
		Document entity = getResponseEntityAsDocument(response, null);
		QName recordName = new QName(Namespaces.CSW, "Record");
		NodeList recordList = entity.getElementsByTagNameNS(
				recordName.getNamespaceURI(), recordName.getLocalPart());
		Assert.assertTrue(recordList.getLength() > 0, ErrorMessage.format(
				ErrorMessageKeys.EMPTY_RESULT_SET, recordName));
		// NOTE: Spec does not indicate how records are matched
		// ETSAssert.assertAllTermsOccur(recordList, keyword);
	}

	/**
	 * [Test] Submits a GetRecords request where the <code>q</code> parameter
	 * contains a subject word and the <code>maxRecords</code> parameter value
	 * has the value "0" (zero). The csw:SearchResults element in the response
	 * must be empty.
	 */
	@Test(description = "Requirements: 086")
	public void getRecordsBySubjectWithoutResults() {
		if (this.recordTopics.isEmpty()) {
			throw new SkipException(
					"No dc:subject elements found in sample records.");
		}
		Map<String, String> qryParams = new HashMap<>();
		qryParams.put(CAT3.REQUEST, CAT3.GET_RECORDS);
		qryParams.put(CAT3.SERVICE, CAT3.SERVICE_TYPE_CODE);
		qryParams.put(CAT3.VERSION, CAT3.VERSION_3_0_0);
		qryParams.put(CAT3.TYPE_NAMES, "Record");
		qryParams.put(CAT3.MAX_RECORDS, "0");
		qryParams.put(CAT3.ELEMENT_SET, CAT3.ELEMENT_SET_SUMMARY);
		int randomIndex = ThreadLocalRandom.current().nextInt(
				this.recordTopics.size());
		String[] subjectWords = this.recordTopics.get(randomIndex)
				.split("\\s+");
		String subject = subjectWords[subjectWords.length - 1];
		qryParams.put(CAT3.Q, subject);
		request = ClientUtils.buildGetRequest(this.getURI, qryParams,
				MediaType.APPLICATION_XML_TYPE);
		response = this.client.handle(request);
		Assert.assertEquals(response.getStatus(),
				ClientResponse.Status.OK.getStatusCode(),
				ErrorMessage.get(ErrorMessageKeys.UNEXPECTED_STATUS));
		Document entity = getResponseEntityAsDocument(response, null);
		Element results = (Element) entity.getElementsByTagNameNS(
				Namespaces.CSW, "SearchResults").item(0);
		Assert.assertNotNull(results, ErrorMessage.format(
				ErrorMessageKeys.MISSING_INFOSET_ITEM, "csw:SearchResults"));
		Assert.assertEquals(results.getChildNodes().getLength(), 0,
				ErrorMessage.format(ErrorMessageKeys.RESULT_SET_SIZE,
						"csw:SummaryRecord"));
		Assert.assertEquals(Integer.parseInt(results
				.getAttribute(CAT3.NUM_REC_RETURNED)), 0, ErrorMessage
				.format(ErrorMessageKeys.INFOSET_ITEM_VALUE,
						"@numberOfRecordsReturned"));
		Assert.assertTrue(Integer.parseInt(results
				.getAttribute(CAT3.NUM_REC_MATCHED)) > 0, ErrorMessage.format(
				ErrorMessageKeys.CONSTRAINT_VIOLATION,
				"numberOfRecordsMatched > 0"));
	}

	/**
	 * [Test] Submits a GetRecords request where the 'q' parameter value is
	 * randomly generated text. The result set is expected to be empty.
	 */
	@Test(description = "OGC 12-176, Table 6 - Text search")
	public void textSearchProducesEmptyResultSet() {
		Map<String, String> qryParams = new HashMap<>();
		qryParams.put(CAT3.REQUEST, CAT3.GET_RECORDS);
		qryParams.put(CAT3.SERVICE, CAT3.SERVICE_TYPE_CODE);
		qryParams.put(CAT3.VERSION, CAT3.VERSION_3_0_0);
		qryParams.put(CAT3.TYPE_NAMES, "Record");
		qryParams.put(CAT3.ELEMENT_SET, CAT3.ELEMENT_SET_FULL);
		qryParams.put(CAT3.Q, Records.generateRandomText());
		request = ClientUtils.buildGetRequest(this.getURI, qryParams,
				MediaType.APPLICATION_XML_TYPE);
		response = this.client.handle(request);
		Assert.assertEquals(response.getStatus(),
				ClientResponse.Status.OK.getStatusCode(),
				ErrorMessage.get(ErrorMessageKeys.UNEXPECTED_STATUS));
		Document entity = getResponseEntityAsDocument(response, null);
		ETSAssert.assertEmptyResultSet(entity);
	}

	/**
	 * [Test] This test submits a GetRecords request where the 'q' parameter
	 * value contains two terms. The terms are separated by a space character,
	 * and the parameter value as a whole is percent-encoded. The result set
	 * must not be empty.
	 */
	@Test(description = "OGC 12-176, Table 6 - Text search")
	public void multipleTermTextSearch() {
		Map<String, String> qryParams = new HashMap<>();
		qryParams.put(CAT3.REQUEST, CAT3.GET_RECORDS);
		qryParams.put(CAT3.SERVICE, CAT3.SERVICE_TYPE_CODE);
		qryParams.put(CAT3.VERSION, CAT3.VERSION_3_0_0);
		qryParams.put(CAT3.TYPE_NAMES, "Record");
		qryParams.put(CAT3.ELEMENT_SET, CAT3.ELEMENT_SET_SUMMARY);
		QName titleName = new QName(Namespaces.DCMES, "title");
		QName subjectName = new QName(Namespaces.DCMES, "subject");
		String searchTerms = Records.findMatchingSearchTerms(this.datasetInfo.getDataFile(), titleName, subjectName);
		qryParams.put(CAT3.Q, URIUtils.getPercentEncodedString(searchTerms));
		request = ClientUtils.buildGetRequest(this.getURI, qryParams,
				MediaType.APPLICATION_XML_TYPE);
		response = this.client.handle(request);
		Assert.assertEquals(response.getStatus(),
				ClientResponse.Status.OK.getStatusCode(),
				ErrorMessage.get(ErrorMessageKeys.UNEXPECTED_STATUS));
		Document entity = getResponseEntityAsDocument(response, null);
		QName recordName = new QName(Namespaces.CSW, "SummaryRecord");
		NodeList recordList = entity.getElementsByTagNameNS(recordName.getNamespaceURI(), recordName.getLocalPart());
		Assert.assertTrue(recordList.getLength() > 0, ErrorMessage.format(ErrorMessageKeys.EMPTY_RESULT_SET, recordName));
		// NOTE: Spec does not indicate how multiple terms are interpreted or
		// how records are matched
		// ETSAssert.assertAllTermsOccur(recordList, searchTerms.split("\\s+"));
	}

	/**
	 * [Test] Submits a GetRecords request containing the <code>bbox</code> and
	 * <code>q</code> parameters, where the latter matches one or more record
	 * titles. The response must include all records that satisfy both search
	 * criteria.
	 *
	 * @see "OGC Catalogue Services 3.0 Specification - HTTP Protocol Binding,
	 *      6.5.5.3: KVP encoding"
	 */
	@Test(description = "Requirements: Table 6")
	public void getRecordsByBBOXAndTitle() {
		if (null == this.geoExtent) {
			throw new SkipException(
					"Could not determine extent of sample data.");
		}
		int maxRecords = 15;
		Map<String, String> qryParams = new HashMap<>();
		qryParams.put(CAT3.REQUEST, CAT3.GET_RECORDS);
		qryParams.put(CAT3.SERVICE, CAT3.SERVICE_TYPE_CODE);
		qryParams.put(CAT3.VERSION, CAT3.VERSION_3_0_0);
		qryParams.put(CAT3.TYPE_NAMES, "Record");
		qryParams.put(CAT3.ELEMENT_SET, CAT3.ELEMENT_SET_SUMMARY);
		qryParams.put(CAT3.MAX_RECORDS, Integer.toString(maxRecords));
		Envelope bbox = this.geoExtent;
		try {
			if (!bbox.getCoordinateReferenceSystem().equals(
					DefaultGeographicCRS.WGS84)) {
				bbox = new GeneralEnvelope(Envelopes.transform(bbox,
						DefaultGeographicCRS.WGS84));
			}
		} catch (TransformException ex) {
			throw new RuntimeException("Failed to create WGS84 envelope.", ex);
		}
		qryParams.put(CAT3.BBOX, Extents.envelopeToString(bbox));
		String titleWord = null;
		try { // get titles for records with bbox
			XdmValue titles = this.datasetInfo
					.findItems(
							"//csw:Record[ows:BoundingBox or ows:WGS84BoundingBox]/dc:title",
							null);
			for (XdmItem title : titles) {
				if (!title.getStringValue().isEmpty()) {
					String[] titleWords = title.getStringValue().trim()
							.split("\\s+");
					titleWord = titleWords[0];
					break;
				}
			}
		} catch (SaxonApiException ex) {
			throw new RuntimeException(ex.getMessage());
		}
		// remove any chars that may give rise to invalid XPath expression
		titleWord = titleWord.replaceAll("[()]", "");
		qryParams.put(CAT3.Q, titleWord);
		request = ClientUtils.buildGetRequest(this.getURI, qryParams,
				MediaType.APPLICATION_XML_TYPE);
		response = this.client.handle(request);
		Assert.assertEquals(response.getStatus(),
				ClientResponse.Status.OK.getStatusCode(),
				ErrorMessage.get(ErrorMessageKeys.UNEXPECTED_STATUS));
		Document entity = getResponseEntityAsDocument(response, null);
		Element results = (Element) entity.getElementsByTagNameNS(
				Namespaces.CSW, "SearchResults").item(0);
		Assert.assertNotNull(results, ErrorMessage.format(
				ErrorMessageKeys.MISSING_INFOSET_ITEM, "csw:SearchResults"));
		String numReturned = results.getAttribute(CAT3.NUM_REC_RETURNED);
		Assert.assertTrue(Integer.parseInt(numReturned) <= maxRecords,
				ErrorMessage.format(ErrorMessageKeys.CONSTRAINT_VIOLATION,
						"numberOfRecordsReturned <= maxRecords"));
		ETSAssert.assertEnvelopeIntersectsBoundingBoxes(bbox, new DOMSource(
				results));
		QName recordName = new QName(Namespaces.CSW, "SummaryRecord");
		NodeList recordList = results.getElementsByTagNameNS(
				recordName.getNamespaceURI(), recordName.getLocalPart());
		Assert.assertTrue(recordList.getLength() > 0, ErrorMessage.format(
				ErrorMessageKeys.EMPTY_RESULT_SET, recordName));
		ETSAssert.assertAllTermsOccur(recordList, titleWord);
	}
}
