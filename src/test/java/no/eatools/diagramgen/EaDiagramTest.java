package no.eatools.diagramgen;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sparx.Package;

import java.util.List;

import no.eatools.util.EaApplicationProperties;

/**
 * Unit tests for the EaDiagram class. Note that these tests rely on the model defined in the
 */
public class EaDiagramTest extends AbtractEaTestCase {

    /** The Constant log. */
    private static final Log log = LogFactory.getLog(EaDiagramTest.class);

    /**
     * Test find diagrams in package.
     * 
     * @throws Exception the exception
     */
    public void testFindDiagramsInPackage() throws Exception {
        Package rootPkg = eaRepo.getRootPackage();
        Package thePkg = eaRepo.findPackageByName("Domain Model", rootPkg, EaRepo.RECURSIVE);
        assertNotNull(thePkg);

        List<EaDiagram> diagrams = EaDiagram.findDiagramsInPackage(eaRepo, thePkg);
        assertNotNull(diagrams);
    }

    /**
     * Test logical path name.
     * 
     * @throws Exception the exception
     */
    public void testLogicalPathName() throws Exception {
        EaDiagram diagram = EaDiagram.findDiagram(eaRepo, "Domain Model");
        assertNotNull(diagram);
        String filename = diagram.getPathname();
        assertEquals("\\Model\\Domain Model", filename);
    }

    /**
     * Test generate diagram.
     */
    public void testGenerateDiagram() {
        //EaDiagram diagram = EaDiagram.findDiagram(eaRepo, "Domain Model");
        String diagramName = EaApplicationProperties.EA_DIAGRAM_TO_GENERATE.value();
        if (diagramName.equals(""))
            diagramName = "Domain Model";
        EaDiagram diagram = EaDiagram.findDiagram(eaRepo, diagramName);
        if (diagram != null) {
            boolean didCreate = diagram.writeImageToFile(EaApplicationProperties.EA_DOC_ROOT_DIR.value());
            assertTrue(didCreate);
        } else {
            fail();
        }
    }

    /**
     * Test generate all diagrams in package.
     * 
     * @throws Exception the exception
     */
    public void testGenerateAllDiagramsInPackage() throws Exception {
        Package pkg = eaRepo.findPackageByName("Klasser", EaRepo.RECURSIVE);

        for (ImageFileFormat imageFileFormat : ImageFileFormat.values()) {
            List<EaDiagram> diagrams = EaDiagram.findDiagramsInPackage(eaRepo, pkg);
            for (EaDiagram d : diagrams) {
                boolean didCreate = d.writeImageToFile(imageFileFormat);
                assertTrue(didCreate);
            }
        }
    }

    /**
     * Test generate all diagrams in project.
     * 
     * @throws Exception the exception
     */
    public void testGenerateAllDiagramsInProject() throws Exception {
        int count = EaDiagram.generateAll(eaRepo);
        log.debug("Generated " + count + " diagrams");
        assertTrue(count > 0);
    }
}
