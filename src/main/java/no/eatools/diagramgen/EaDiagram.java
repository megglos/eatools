package no.eatools.diagramgen;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import no.eatools.util.EaApplicationProperties;
import no.eatools.util.IntCounter;
import no.eatools.util.SystemProperties;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.sparx.Diagram;
import org.sparx.Package;

/**
 * A Wrapper class to facilitate Diagram generation and manipulation.
 * 
 * @author Per Spilling (per.spilling@objectware.no)
 */
public class EaDiagram {
    private static final Logger log = Logger.getLogger(EaDiagram.class);
    private Diagram eaDiagram;
    private ImageFileFormat defaultImageFormat = ImageFileFormat.PNG;
    private EaRepo eaRepo;
    private String logicalPathname;

    /**
     * Generate all diagrams from the model into the directory configured in the properties.
     * The package structure of the model is retained as directory structure.
     * All existing diagrams are overwritten.
     * 
     * @param eaRepo the ea repo
     * @return the int
     */
    public static int generateAll(EaRepo eaRepo) {
        return generateAll(eaRepo, EaApplicationProperties.EA_DOC_ROOT_DIR.value());
    }

    /**
     * Generate all diagrams from the model into the specified output directory.
     * The package structure of the model is retained as directory structure.
     * All existing diagrams are overwritten.
     * 
     * @param eaRepo the ea repo
     * @param outputDir the output dir
     * @return the int
     */
    public static int generateAll(EaRepo eaRepo, String outputDir) {
        IntCounter count = new IntCounter();
        generateAllDiagrams(eaRepo, outputDir, eaRepo.getRootPackage(), count);
        return count.count;
    }

    /**
     * Recursive method that finds all diagrams in a package and writes them to file.
     * 
     * @param repo
     * @param pkg
     * @param diagramCount
     */
    private static void generateAllDiagrams(EaRepo repo, String customOutputDir, Package pkg, IntCounter diagramCount) {
        final List<EaDiagram> diagrams = findDiagramsInPackage(repo, pkg);
        final String outputDir = customOutputDir != null ? customOutputDir : EaApplicationProperties.EA_DOC_ROOT_DIR.value();
        if (diagrams.size() > 0) {
            log.debug("Generating diagrams in package: " + pkg.GetName());
            diagramCount.count = diagramCount.count + diagrams.size();
            for (EaDiagram d : diagrams) {
                log.debug("Generating diagrams: " + d.getFilename());
                d.writeImageToFile(outputDir);
            }
        }
        for (Package p : pkg.GetPackages()) {
            generateAllDiagrams(repo, outputDir, p, diagramCount);
        }
    }

    /**
     * Generate specific diagram.
     * 
     * @param eaRepo the ea repo
     * @param customOutputDir the custom output dir
     */
    public static void generateSpecificDiagram(final EaRepo eaRepo, final String customOutputDir) {
        final String diagramName = EaApplicationProperties.EA_DIAGRAM_TO_GENERATE.value();
        final EaDiagram diagram = EaDiagram.findDiagram(eaRepo, diagramName);
        final String outputDir = customOutputDir != null ? customOutputDir : EaApplicationProperties.EA_DOC_ROOT_DIR.value();
        if (diagram != null) {
            if (outputDir != null) {
                diagram.writeImageToFile(outputDir);
            } else {
                diagram.writeImageToFile(outputDir);
            }
        } else {
            log.info("diagram '" + diagramName + "' not found");
        }
    }

    /**
     * Find diagram.
     * 
     * @param eaRepo the ea repo
     * @param diagramName the diagram name
     * @return the ea diagram
     */
    public static EaDiagram findDiagram(EaRepo eaRepo, String diagramName) {
        return findDiagram(eaRepo, eaRepo.getRootPackage(), diagramName, true);
    }

    private static EaDiagram findDiagram(EaRepo eaRepo, Package pkg, String diagramName, boolean recursive) {
        if (pkg == null) {
            return null;
        }
        for (Diagram diagram : pkg.GetDiagrams()) {
            //log.debug("Diagram name = " + diagram.GetName());
            if (diagram.GetName().equals(diagramName)) {
                return new EaDiagram(eaRepo, diagram, getPackagePath(eaRepo, pkg));
            }
        }
        if (recursive) {
            for (Package p : pkg.GetPackages()) {
                EaDiagram d = findDiagram(eaRepo, p, diagramName, recursive);
                if (d != null) {
                    return d;
                }
            }
        }
        return null;
    }

    private static String getPackagePath(EaRepo eaRepo, Package pkg) {
        ArrayList<Package> ancestorPackages = new ArrayList<Package>();
        getAncestorPackages(ancestorPackages, eaRepo, pkg);
        StringBuffer pathName = new StringBuffer();
        Collections.reverse(ancestorPackages);
        for (Package p : ancestorPackages) {
            pathName.append(SystemProperties.FILE_SEPARATOR.value() + p.GetName());
        }
        return pathName.toString();
    }

    private static String makeWebFriendlyFilename(String s) {
        s = StringUtils.replaceChars(s, ' ', '_');
        s = StringUtils.replaceChars(s, '/', '-');
        /* Replace Norwegian characters with alternatives */
        s = StringUtils.replace(s, "Æ", "ae");
        s = StringUtils.replace(s, "Ø", "oe");
        s = StringUtils.replace(s, "Å", "aa");
        s = StringUtils.replace(s, "æ", "ae");
        s = StringUtils.replace(s, "ø", "oe");
        s = StringUtils.replace(s, "å", "aa");
        s = StringUtils.lowerCase(s);
        return s;
    }

    private static void getAncestorPackages(ArrayList<Package> ancestorPackages, EaRepo eaRepo, Package pkg) {
        ancestorPackages.add(pkg);
        if (pkg.GetParentID() != 0) {
            getAncestorPackages(ancestorPackages, eaRepo, eaRepo.findPackageByID(pkg.GetParentID()));
        }
    }

    /**
     * Find all UML diagrams inside a specific Package. Non-recursive, searches the top-level (given)
     * package only.
     * 
     * @param eaRepo the ea repo
     * @param pkg the Package to serach in.
     * @return the list
     */
    public static List<EaDiagram> findDiagramsInPackage(EaRepo eaRepo, Package pkg) {
        if (pkg == null) {
            return Collections.EMPTY_LIST;
        }
        List<EaDiagram> result = new ArrayList<EaDiagram>();
        for (Diagram d : pkg.GetDiagrams()) {
            result.add(new EaDiagram(eaRepo, d, getPackagePath(eaRepo, pkg)));
        }
        return result;
    }

    /**
     * Instantiates a new ea diagram.
     * 
     * @param repository the repository
     * @param diagram the diagram
     * @param pathName the path name
     */
    public EaDiagram(EaRepo repository, Diagram diagram, String pathName) {
        eaDiagram = diagram;
        eaRepo = repository;
        logicalPathname = pathName;
    }

    /**
     * Instantiates a new ea diagram.
     * 
     * @param repository the repository
     * @param diagram the diagram
     * @param pathName the path name
     * @param imageFormat the image format
     */
    public EaDiagram(EaRepo repository, Diagram diagram, String pathName, ImageFileFormat imageFormat) {
        eaDiagram = diagram;
        eaRepo = repository;
        logicalPathname = pathName;
        defaultImageFormat = imageFormat;
    }

    /**
     * Write image to file.
     * 
     * @param outputPath the output path
     * @return true, if successful
     */
    public boolean writeImageToFile(final String outputPath) {
        return writeImageToFile(outputPath, defaultImageFormat);
    }

    /**
     * Write image to file.
     * 
     * @param imageFileFormat the image file format
     * @return true, if successful
     */
    public boolean writeImageToFile(final ImageFileFormat imageFileFormat) {
        return writeImageToFile(EaApplicationProperties.EA_DOC_ROOT_DIR.value(), imageFileFormat);
    }

    /**
     * Write image to file.
     * 
     * @param outputPath the output path
     * @param imageFileFormat the image file format
     * @return true, if successful
     */
    public boolean writeImageToFile(final String outputPath, final ImageFileFormat imageFileFormat) {
        // make sure the directory exists
        File f = new File(getAbsolutePathName(outputPath));
        f.mkdirs();
        String diagramFileName = getAbsoluteFilename(outputPath);
        if (eaRepo.getProject().PutDiagramImageToFile(eaDiagram.GetDiagramGUID(), diagramFileName, imageFileFormat.isRaster())) {
            log.info("Diagram generated at: " + diagramFileName);
            return true;
        } else {
            log.error("Unable to create diagram:" + diagramFileName);
            return false;
        }
    }

    public String getPathname() {
        return logicalPathname;
    }

    /**
     * Gets the absolute path name.
     * 
     * @param path the path
     * @return the absolute path name
     */
    public String getAbsolutePathName(final String path) {
        return makeWebFriendlyFilename(path + logicalPathname);
    }

    public String getFilename() {
        return makeWebFriendlyFilename(eaDiagram.GetName() + defaultImageFormat.getFileExtension());
    }

    /**
     * Gets the absolute filename.
     * 
     * @param path the path
     * @return the absolute filename
     */
    public String getAbsoluteFilename(final String path) {
        return getAbsolutePathName(path) + SystemProperties.FILE_SEPARATOR.value() + getFilename();
    }
}
