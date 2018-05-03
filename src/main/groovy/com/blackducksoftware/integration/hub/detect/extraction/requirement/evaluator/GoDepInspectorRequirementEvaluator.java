package com.blackducksoftware.integration.hub.detect.extraction.requirement.evaluator;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.blackducksoftware.integration.hub.detect.DetectConfiguration;
import com.blackducksoftware.integration.hub.detect.exception.DetectUserFriendlyException;
import com.blackducksoftware.integration.hub.detect.extraction.requirement.GoDepInspectorRequirement;
import com.blackducksoftware.integration.hub.detect.extraction.requirement.evaluation.EvaluationContext;
import com.blackducksoftware.integration.hub.detect.extraction.requirement.evaluation.RequirementEvaluation;
import com.blackducksoftware.integration.hub.detect.extraction.requirement.evaluation.RequirementEvaluation.EvaluationResult;
import com.blackducksoftware.integration.hub.detect.type.ExecutableType;
import com.blackducksoftware.integration.hub.detect.util.DetectFileManager;
import com.blackducksoftware.integration.hub.detect.util.executable.Executable;
import com.blackducksoftware.integration.hub.detect.util.executable.ExecutableManager;
import com.blackducksoftware.integration.hub.detect.util.executable.ExecutableRunner;
import com.blackducksoftware.integration.hub.detect.util.executable.ExecutableRunnerException;

@Component
public class GoDepInspectorRequirementEvaluator extends RequirementEvaluator<GoDepInspectorRequirement> {
    private final Logger logger = LoggerFactory.getLogger(GoDepInspectorRequirementEvaluator.class);

    @Autowired
    public DetectFileManager detectFileManager;

    @Autowired
    public DetectConfiguration detectConfiguration;

    @Autowired
    public ExecutableManager executableManager;

    @Autowired
    public ExecutableRunner executableRunner;

    private boolean hasResolvedInspector;
    private String resolvedGoDep;

    @Override
    public RequirementEvaluation<String> evaluate(final GoDepInspectorRequirement requirement, final EvaluationContext context) {
        try {
            if (!hasResolvedInspector) {
                resolvedGoDep = install();
            }

            if (resolvedGoDep != null) {
                return new RequirementEvaluation<>(EvaluationResult.Passed, resolvedGoDep);
            } else {
                return new RequirementEvaluation<>(EvaluationResult.Failed, null);
            }
        }catch (final Exception e) {
            return new RequirementEvaluation<>(EvaluationResult.Exception, e);
        }
    }

    @Override
    public Class getRequirementClass() {
        return GoDepInspectorRequirement.class;
    }

    public String install() throws DetectUserFriendlyException, ExecutableRunnerException, IOException {
        String goDepPath = detectConfiguration.getGoDepPath();
        if (StringUtils.isBlank(goDepPath)) {
            final File goDep = getGoDepInstallLocation();
            if (goDep.exists()) {
                goDepPath = goDep.getAbsolutePath();
            } else {
                goDepPath = executableManager.getExecutablePath(ExecutableType.GO_DEP, true, detectConfiguration.getSourcePath());
            }
        }
        if (StringUtils.isBlank(goDepPath)) {
            final String goExecutable = executableManager.getExecutablePath(ExecutableType.GO, true, detectConfiguration.getSourcePath());
            goDepPath = installGoDep(goExecutable);
        }
        return goDepPath;
    }

    private String installGoDep(final String goExecutable) throws ExecutableRunnerException {
        final File goDep = getGoDepInstallLocation();
        final File installDirectory = goDep.getParentFile();
        installDirectory.mkdirs();
        logger.debug("Retrieving the Go Dep tool");

        final Executable getGoDep = new Executable(installDirectory, goExecutable, Arrays.asList(
                "get",
                "-u",
                "-v",
                "-d",
                "github.com/golang/dep/cmd/dep"
                ));
        executableRunner.execute(getGoDep);

        logger.debug("Building the Go Dep tool in ${goOutputDirectory}");
        final Executable buildGoDep = new Executable(installDirectory, goExecutable, Arrays.asList(
                "build",
                "github.com/golang/dep/cmd/dep"
                ));
        executableRunner.execute(buildGoDep);

        return goDep.getAbsolutePath();
    }

    private File getGoDepInstallLocation() {
        final File goOutputDirectory = new File(detectConfiguration.getOutputDirectory(), "Go");
        return new File(goOutputDirectory, executableManager.getExecutableName(ExecutableType.GO_DEP));
    }


}