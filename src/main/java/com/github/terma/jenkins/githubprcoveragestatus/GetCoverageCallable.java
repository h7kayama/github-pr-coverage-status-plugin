/*

    Copyright 2015-2016 Artem Stasiuk

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

*/
package com.github.terma.jenkins.githubprcoveragestatus;

import hudson.FilePath;
import hudson.Util;
import hudson.remoting.VirtualChannel;
import jenkins.MasterToSlaveFileCallable;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("WeakerAccess")
final class GetCoverageCallable extends MasterToSlaveFileCallable<Map<String, Float>> implements CoverageRepository {

    private final boolean disableSimpleCov;

    GetCoverageCallable(final boolean disableSimpleCov) {
        this.disableSimpleCov = disableSimpleCov;
    }

    private static List<Float> getFloats(File ws, String path, CoverageReportParser parser) {
        FileSet fs = Util.createFileSet(ws, path);
        DirectoryScanner ds = fs.getDirectoryScanner();
        String[] files = ds.getIncludedFiles();
        List<Float> cov = new ArrayList<Float>();
        for (String file : files) cov.add(parser.get(new File(ds.getBasedir(), file).getAbsolutePath()));
        return cov;
    }

    @Override
    public Map<String, Float> get(final FilePath workspace) throws IOException, InterruptedException {
        if (workspace == null) throw new IllegalArgumentException("Workspace should not be null!");
        return workspace.act(new GetCoverageCallable(disableSimpleCov));
    }

    @Override
    public Map<String, Float> invoke(final File ws, final VirtualChannel channel) throws IOException {
        final Map<String, Float> cov = new HashMap<String, Float>();
        cov.put("frontend", averageCoverage(getFloats(ws, "**/cobertura-coverage.xml", new CoberturaParser())));
        if (!disableSimpleCov) cov.put("backend", averageCoverage(getFloats(ws, "**/coverage.json", new SimpleCovParser())));
        return cov;
    }

    private float averageCoverage(final List<Float> cov) {
        float s = 0;
        for (float v : cov) s += v;

        if (cov.isEmpty()) {
            return 0f;
        } else {
            return s / cov.size();
        }
    }

}
