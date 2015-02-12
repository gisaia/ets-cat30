<?xml version="1.0" encoding="UTF-8"?>
<iso:schema id="csw-capabilities-3.0" 
  schemaVersion="3.0.0"
  xmlns:iso="http://purl.oclc.org/dsdl/schematron" 
  xml:lang="en"
  queryBinding="xslt2">

  <iso:title>CSW 3.0 Capabilities</iso:title>

  <iso:ns prefix="ows" uri="http://www.opengis.net/ows/2.0" />
  <iso:ns prefix="csw" uri="http://www.opengis.net/cat/csw/3.0" />
  <iso:ns prefix="fes" uri="http://www.opengis.net/fes/2.0" />
  <iso:ns prefix="xlink" uri="http://www.w3.org/1999/xlink" />

  <iso:p>This Schematron (ISO 19757-3) schema specifies constraints regarding 
  the content of CSW 3.0 service capabilities descriptions.</iso:p>

  <iso:let name="CSW_NS" value="'http://www.opengis.net/cat/csw/3.0'" />
  <iso:let name="ATOM_NS" value="'http://www.w3.org/2005/Atom'" />

  <iso:phase id="BasicCataloguePhase">
    <iso:active pattern="EssentialCapabilitiesPattern"/>
    <iso:active pattern="TopLevelElementsPattern"/>
    <iso:active pattern="ServiceConstraintsPattern"/>
    <iso:active pattern="ServiceIdentificationPattern"/>
    <iso:active pattern="BasicCataloguePattern"/>
    <iso:active pattern="OperationPattern"/>
  </iso:phase>

  <iso:pattern id="EssentialCapabilitiesPattern">
    <iso:rule context="/*[1]">
      <iso:assert test="local-name(.) = 'Capabilities'" 
        diagnostics="dmsg.local-name">
        The document element must have [local name] = "Capabilities".
      </iso:assert>
      <iso:assert test="namespace-uri(.) = 'http://www.opengis.net/cat/csw/3.0'" 
        diagnostics="dmsg.ns-name">
        The document element must have [namespace name] = "http://www.opengis.net/cat/csw/3.0".
      </iso:assert>
      <iso:assert test="@version = '3.0.0'" diagnostics="dmsg.version.en">
        The capabilities document must have @version = "3.0.0".
      </iso:assert>
    </iso:rule>
  </iso:pattern>

  <iso:pattern id="TopLevelElementsPattern">
    <iso:p>Rules regarding the inclusion of common service metadata elements.</iso:p>
    <iso:rule context="/*[1]">
      <iso:assert test="ows:ServiceIdentification">The ows:ServiceIdentification element is missing.</iso:assert>
      <iso:assert test="ows:ServiceProvider">The ows:ServiceProvider element is missing.</iso:assert>
      <iso:assert test="ows:OperationsMetadata">The ows:OperationsMetadata element is missing.</iso:assert>
      <iso:assert test="ows:Languages">The ows:Languages element is missing.</iso:assert>
      <iso:assert test="fes:Filter_Capabilities">The fes:Filter_Capabilities element is missing.</iso:assert>
    </iso:rule>
  </iso:pattern>

  <iso:pattern id="ServiceConstraintsPattern">
    <iso:p>Implementation conformance statement. See OGC 12-176r6: Table 17.</iso:p>
    <iso:rule id="R48" context="ows:OperationsMetadata">
      <iso:assert test="ows:Constraint[ends-with(@name,'OpenSearch')]/ows:DefaultValue or 
      ows:Constraint[ends-with(@name,'OpenSearch')]//ows:Value">
      No ows:Constraint value found for conformance class 'OpenSearch'.
      </iso:assert>
      <iso:assert test="ows:Constraint[ends-with(@name,'GetCapabilities-XML')]/ows:DefaultValue or 
      ows:Constraint[ends-with(@name,'GetCapabilities-XML')]//ows:Value">
      No ows:Constraint value found for conformance class 'GetCapabilities-XML'.
      </iso:assert>
      <iso:assert test="ows:Constraint[ends-with(@name,'GetRecordById-XML')]/ows:DefaultValue or 
      ows:Constraint[ends-with(@name,'GetRecordById-XML')]//ows:Value">
      No ows:Constraint value found for conformance class 'GetRecordById-XML'.
      </iso:assert>
      <!-- TODO: Add assertions for the remaining conformance classes -->
    </iso:rule>
  </iso:pattern>

  <iso:pattern id="BasicCataloguePattern">
    <iso:p>Basic-Catalogue conformance class. See OGC 12-176r6: Table 1.</iso:p>
    <iso:rule context="ows:OperationsMetadata">
      <iso:assert test="ows:Operation[@name='GetCapabilities']//ows:Get/@xlink:href">
      The GET method endpoint for GetCapabilities is missing.
      </iso:assert>
      <iso:assert test="ows:Operation[@name='GetRecordById']">
      The mandatory GetRecordById operation is missing.
      </iso:assert>
      <iso:assert test="ows:Operation[@name='GetRecords']">
      The mandatory GetRecords operation is missing.
      </iso:assert>
    </iso:rule>
    <iso:rule context="fes:Filter_Capabilities/fes:Conformance">
      <iso:assert test="upper-case(fes:Constraint[@name='ImplementsMinSpatialFilter']/ows:DefaultValue) = 'TRUE'">
      The filter constraint 'ImplementsMinSpatialFilter' must be 'TRUE' for all conforming implementations.
      </iso:assert>
    </iso:rule>
  </iso:pattern>

  <iso:pattern id="OperationPattern">
    <iso:p>Constraints that apply to Operation elements.</iso:p>
    <iso:rule id="R137-R138" context="ows:Operation[@name='GetRecordById']">
      <iso:assert test="ows:Parameter[matches(@name,'outputSchema','i')]//ows:Value[1] eq $CSW_NS">
      GetRecordById: the first allowed value of the outputSchema parameter must be '<iso:value-of select="$CSW_NS"/>'.
      </iso:assert>
      <iso:assert test="ows:Parameter[matches(@name,'outputSchema','i')]//ows:Value eq $ATOM_NS">
      GetRecordById: outputSchema parameter must allow '<iso:value-of select="$ATOM_NS"/>'.
      </iso:assert>
    </iso:rule>
  </iso:pattern>

  <iso:pattern id="ServiceIdentificationPattern">
    <iso:rule context="ows:ServiceIdentification">
      <iso:assert test="upper-case(ows:ServiceType) = 'CSW'"
        diagnostics="dmsg.serviceType.en"> 
        The value of the ows:ServiceType element must be "CSW".
      </iso:assert>
      <iso:assert test="ows:ServiceTypeVersion = '3.0.0'" 
        diagnostics="dmsg.serviceTypeVersion.en">
        An ows:ServiceTypeVersion element having the value "3.0.0" must be present.
      </iso:assert>
    </iso:rule>
  </iso:pattern>

  <iso:diagnostics>
    <iso:diagnostic id="dmsg.local-name" xml:lang="en">
      The root element has [local name] = '<iso:value-of select="local-name(.)"/>'.
    </iso:diagnostic>
    <iso:diagnostic id="dmsg.ns-name" xml:lang="en">
      The element has [namespace name] = '<iso:value-of select="namespace-uri(.)"/>'.
    </iso:diagnostic>
    <iso:diagnostic id="dmsg.version.en" xml:lang="en">
    The reported version is <iso:value-of select="/*[1]/@version"/>.
    </iso:diagnostic>
    <iso:diagnostic id="dmsg.serviceType.en" xml:lang="en">
    The reported ServiceType is '<iso:value-of select="./ows:ServiceType"/>'.
    </iso:diagnostic>
    <iso:diagnostic id="dmsg.serviceTypeVersion.en" xml:lang="en">
    The reported ServiceTypeVersion is <iso:value-of select="./ows:ServiceTypeVersion"/>.
    </iso:diagnostic>
  </iso:diagnostics>

</iso:schema>