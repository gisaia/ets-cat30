package org.opengis.cite.cat30;

import java.net.URI;

/**
 * XML namespace names.
 *
 * @see <a href="http://www.w3.org/TR/xml-names/">Namespaces in XML 1.0</a>
 *
 */
public class Namespaces {

    private Namespaces() {
    }

    /**
     * SOAP 1.2 message envelopes.
     */
    public static final String SOAP_ENV = "http://www.w3.org/2003/05/soap-envelope";
    /**
     * W3C XLink
     */
    public static final String XLINK = "http://www.w3.org/1999/xlink";
    /**
     * OGC 06-121r9 (OWS 2.0)
     */
    public static final String OWS = "http://www.opengis.net/ows/2.0";
    /**
     * OGC 06-121r3 (OWS 1.1)
     */
    public static final String OWS11 = "http://www.opengis.net/ows/1.1";
    /**
     * GML 3.2 (ISO 19136)
     */
    public static final String GML = "http://www.opengis.net/gml/3.2";
    /**
     * GML 3.1 (03-105r1)
     */
    public static final String GML31 = "http://www.opengis.net/gml";
    /**
     * ISO 19143:2010 (FES 2.0)
     */
    public static final String FES = "http://www.opengis.net/fes/2.0";
    /**
     * OGC 12-176r4 (CSW 3.0)
     */
    public static final String CSW = "http://www.opengis.net/cat/csw/3.0";
    /**
     * DCMI Metadata Terms
     */
    public static final String DCMI = "http://purl.org/dc/terms/";
    /**
     * Dublin Core Metadata Element Set, Version 1.1 (legacy)
     */
    public static final String DCMES = "http://purl.org/dc/elements/1.1/";
    /**
     * Atom Syndication Format (RFC 4287)
     */
    public static final String ATOM = "http://www.w3.org/2005/Atom";
    /**
     * OpenSearch 1.1 description
     */
    public static final String OSD11 = "http://a9.com/-/spec/opensearch/1.1/";
    /**
     * OpenSearch Geo extension namespace (see OGC 10-032r8).
     */
    public static final String OS_GEO = "http://a9.com/-/opensearch/extensions/geo/1.0/";
    /**
     * <a href="http://www.georss.org/" target="_blank">GeoRSS</a> namespace.
     */
    public static final String GEORSS = "http://www.georss.org/georss";
    /**
     * W3C XML Schema namespace
     */
    public static final URI XSD = URI
            .create("http://www.w3.org/2001/XMLSchema");
    /**
     * Schematron (ISO 19757-3) namespace
     */
    public static final URI SCH = URI
            .create("http://purl.oclc.org/dsdl/schematron");

}
