package com.knowledge.base.document.service.impl;

import org.junit.jupiter.api.Test;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the PPTX XML tag balancer
 */
class FileManagementServiceImplTest {

    /**
     * Invokes the private method balanceXmlTags via reflection
     */
    private String invokeBalanceXmlTags(String xml) throws Exception {
        FileManagementServiceImpl impl = new FileManagementServiceImpl();
        Method method = FileManagementServiceImpl.class.getDeclaredMethod("balanceXmlTags", String.class);
        method.setAccessible(true);
        return (String) method.invoke(impl, xml);
    }

    /**
     * Verifies that the XML can be successfully parsed by an XML parser
     */
    private void assertValidXml(String xml) {
        try {
            DocumentBuilderFactory.newInstance().newDocumentBuilder()
                    .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            fail("Fixed XML could not be parsed: " + e.getMessage() + "\nXML:\n" + xml);
        }
    }

    // ==================== Test cases ====================

    @Test
    void testMissingPTxBodyCloseBeforePSp() throws Exception {
        // <p:txBody> is missing </p:txBody>, which should be inserted before </p:sp> closes
        String xml = "<?xml version=\"1.0\"?>\n"
                + "<p:sld xmlns:p=\"http://schemas.openxmlformats.org/presentationml/2006/main\">\n"
                + "  <p:cSld>\n"
                + "    <p:sp>\n"
                + "      <p:txBody>\n"
                + "        <a:p>\n"
                + "          <a:r><a:t>hello</a:t></a:r>\n"
                + "        </a:p>\n"
                + "    </p:sp>\n"
                + "  </p:cSld>\n"
                + "</p:sld>";

        String result = invokeBalanceXmlTags(xml);

        // </p:txBody> should be inserted before </p:sp>
        assertTrue(result.contains("</p:txBody>"), "Should contain the fixed </p:txBody>");
        assertTrue(result.indexOf("</p:txBody>") < result.indexOf("</p:sp>"),
                "</p:txBody> should come before </p:sp>");
        assertValidXml(result);
    }

    @Test
    void testMissingPNvSpPrClose() throws Exception {
        // <p:nvSpPr> is missing </p:nvSpPr>
        String xml = "<?xml version=\"1.0\"?>\n"
                + "<p:sp xmlns:p=\"http://schemas.openxmlformats.org/presentationml/2006/main\">\n"
                + "  <p:nvSpPr>\n"
                + "    <p:cNvPr id=\"1\" name=\"test\"/>\n"
                + "    <p:nvPr/>\n"
                + "  <p:spPr>\n"
                + "    <a:prstGeom prst=\"rect\"/>\n"
                + "  </p:spPr>\n"
                + "</p:sp>";

        String result = invokeBalanceXmlTags(xml);

        // Once p:nvSpPr's children are closed, p:nvSpPr itself should be closed before </p:spPr> or at document end
        assertTrue(result.contains("</p:nvSpPr>"), "Should contain the fixed </p:nvSpPr>");
        assertValidXml(result);
    }

    @Test
    void testNestedMissingCloses() throws Exception {
        // Multiple nested missing closing tags: both a:p and p:txBody are missing
        String xml = "<?xml version=\"1.0\"?>\n"
                + "<p:sp xmlns:p=\"http://schemas.openxmlformats.org/presentationml/2006/main\">\n"
                + "  <p:txBody>\n"
                + "    <a:p>\n"
                + "      <a:r><a:t>text</a:t></a:r>\n"
                + "</p:sp>";

        String result = invokeBalanceXmlTags(xml);

        // a:p should be closed first, then p:txBody, then finally p:sp
        assertTrue(result.contains("</a:p>"), "Should have </a:p>");
        assertTrue(result.contains("</p:txBody>"), "Should have </p:txBody>");
        int apClose = result.indexOf("</a:p>");
        int txBodyClose = result.indexOf("</p:txBody>");
        assertTrue(apClose < txBodyClose, "</a:p> should come before </p:txBody>");
        assertValidXml(result);
    }

    @Test
    void testAlreadyValidXmlShouldNotChange() throws Exception {
        String xml = "<?xml version=\"1.0\"?>\n"
                + "<p:sp xmlns:p=\"http://schemas.openxmlformats.org/presentationml/2006/main\">\n"
                + "  <p:txBody>\n"
                + "    <a:p>\n"
                + "      <a:r><a:t>text</a:t></a:r>\n"
                + "    </a:p>\n"
                + "  </p:txBody>\n"
                + "</p:sp>";

        String result = invokeBalanceXmlTags(xml);

        assertEquals(xml, result, "Valid XML should not be modified");
    }

    @Test
    void testSelfClosingTagsPreserved() throws Exception {
        String xml = "<?xml version=\"1.0\"?>\n"
                + "<p:sp xmlns:p=\"http://schemas.openxmlformats.org/presentationml/2006/main\">\n"
                + "  <p:nvSpPr>\n"
                + "    <p:cNvPr id=\"1\" name=\"t\"/>\n"
                + "    <p:nvPr/>\n"
                + "  </p:nvSpPr>\n"
                + "</p:sp>";

        String result = invokeBalanceXmlTags(xml);

        assertEquals(xml, result, "Valid XML should not be modified");
    }
}
