package com.blackducksoftware.integration.hub.detect.extraction.bomtool.sbt;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.blackducksoftware.integration.hub.bdio.model.externalid.ExternalIdFactory;
import com.blackducksoftware.integration.hub.detect.DetectConfiguration;
import com.blackducksoftware.integration.hub.detect.bomtool.sbt.SbtPackager;
import com.blackducksoftware.integration.hub.detect.bomtool.sbt.models.SbtDependencyModule;
import com.blackducksoftware.integration.hub.detect.bomtool.sbt.models.SbtProject;
import com.blackducksoftware.integration.hub.detect.extraction.Extraction;
import com.blackducksoftware.integration.hub.detect.extraction.Extraction.ExtractionResult;
import com.blackducksoftware.integration.hub.detect.extraction.Extractor;
import com.blackducksoftware.integration.hub.detect.model.BomToolType;
import com.blackducksoftware.integration.hub.detect.model.DetectCodeLocation;
import com.blackducksoftware.integration.hub.detect.util.DetectFileManager;

@Component
public class SbtResolutionCacheExtractor extends Extractor<SbtResolutionCacheContext> {
    private final Logger logger = LoggerFactory.getLogger(SbtResolutionCacheExtractor.class);

    @Autowired
    protected DetectConfiguration detectConfiguration;

    @Autowired
    protected DetectFileManager detectFileManager;

    @Autowired
    ExternalIdFactory externalIdFactory;

    @Override
    public Extraction extract(final SbtResolutionCacheContext context) {
        try {
            final String included = detectConfiguration.getSbtIncludedConfigurationNames();
            final String excluded = detectConfiguration.getSbtExcludedConfigurationNames();

            final int depth = detectConfiguration.getSearchDepth();

            final SbtPackager packager = new SbtPackager(externalIdFactory, detectFileManager);
            final SbtProject project = packager.extractProject(context.directory.toString(), depth, included, excluded);

            final List<DetectCodeLocation> codeLocations = new ArrayList<>();

            for (final SbtDependencyModule module : project.modules) {
                final DetectCodeLocation codeLocation = new DetectCodeLocation.Builder(BomToolType.SBT, module.name, project.projectExternalId, module.graph).bomToolProjectName(project.projectName).bomToolProjectVersionName(project.projectVersion).build();
                codeLocations.add(codeLocation);
            }

            if (codeLocations.size() > 0) {
                return new Extraction(ExtractionResult.Success, codeLocations);
            } else {
                logger.error("Unable to find any dependency information.");
                return new Extraction(ExtractionResult.Failure);
            }

        } catch (final Exception e) {
            return new Extraction(ExtractionResult.Failure, e);
        }
    }

}