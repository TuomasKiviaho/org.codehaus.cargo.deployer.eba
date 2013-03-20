package org.apache.aries.plugin.eba;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.osgi.framework.Constants;

import aQute.lib.osgi.Analyzer;
import aQute.libg.version.Version;
import aQute.libg.version.VersionRange;

/**
 * @goal application
 * @phase package
 * @requiresProject true
 * @requiresDependencyResolution test
 */
public class ApplicationMojo extends MojoSupport
{

    private static final String APPLICATION_NAME = "META-INF/APPLICATION.MF";

    /**
     * @parameter default-value="${project.build.directory}/${project.build.finalName}.eba"
     * @required
     */
    private File outputDirectory;

    public ApplicationMojo()
    {
        super();
    }

    protected void doExecute(List<Artifact> artifacts) throws MojoExecutionException,
        MojoFailureException
    {
        StringBuilder applicationContent = new StringBuilder();
        for (Artifact artifact : artifacts)
        {
            try
            {
                File file = artifact.getFile();
                Manifest manifest = file == null ? null : getManifest(file);
                if (manifest != null)
                {
                    Attributes mainAttributes = manifest.getMainAttributes();
                    String bundleSymbolicName =
                        mainAttributes.getValue(Constants.BUNDLE_SYMBOLICNAME);
                    if (bundleSymbolicName != null)
                    {
                        if (applicationContent.length() > 0)
                        {
                            applicationContent.append(",");
                        }
                        applicationContent.append(bundleSymbolicName);
                        String bundleVersion = mainAttributes.getValue(Constants.BUNDLE_VERSION);
                        if (bundleVersion != null)
                        {
                            Version version = new Version(bundleVersion);
                            String artifactVersion = artifact.getVersion();
                            VersionRange versionRange =
                                new VersionRange(Analyzer.cleanupVersion(artifactVersion));
                            applicationContent.append(';');
                            applicationContent.append(Constants.VERSION_ATTRIBUTE);
                            applicationContent.append('=');
                            applicationContent.append('"');
                            applicationContent.append(versionRange.includes(version)
                                ? versionRange : version);
                            applicationContent.append('"');
                        }
                        boolean optional = artifact.isOptional();
                        if (optional)
                        {
                            applicationContent.append(';');
                            applicationContent.append(Constants.RESOLUTION_DIRECTIVE);
                            applicationContent.append('=');
                            applicationContent.append('"');
                            applicationContent.append(Constants.RESOLUTION_OPTIONAL);
                            applicationContent.append('"');
                        }
                    }
                }
            }
            catch (IOException e)
            {
                throw new MojoExecutionException(artifact.toString(), e);
            }
        }
        String applicationSymbolicName = null;
        String applicationVersion = null;
        if (this.project != null)
        {
            Artifact artifact = this.project.getArtifact();
            try
            {
                File file = artifact.getFile();
                Manifest manifest = file == null ? null : getManifest(file);
                if (manifest != null)
                {
                    Attributes mainAttributes = manifest.getMainAttributes();
                    applicationSymbolicName =
                        mainAttributes.getValue(Constants.BUNDLE_SYMBOLICNAME);
                    applicationVersion = mainAttributes.getValue(Constants.BUNDLE_VERSION);
                }
            }
            catch (IOException e)
            {
                throw new MojoExecutionException(artifact.toString(), e);
            }
        }
        File applicationFile = new File(this.outputDirectory, APPLICATION_NAME);
        try
        {
            Manifest application = new Manifest();
            Attributes mainAttributes = application.getMainAttributes();
            mainAttributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
            mainAttributes.putValue("Application-ManifestVersion", "1.0");
            if (applicationSymbolicName != null)
            {
                mainAttributes.putValue("Application-SymbolicName", applicationSymbolicName);
            }
            if (applicationVersion != null)
            {
                mainAttributes.putValue("Application-Version", applicationVersion);
            }
            if (applicationContent.length() > 0)
            {
                mainAttributes.putValue("Application-Content", applicationContent.toString());
            }
            File parentFile = applicationFile.getParentFile();
            parentFile.mkdirs();
            OutputStream outputStream = new FileOutputStream(applicationFile);
            try
            {
                application.write(outputStream);
            }
            finally
            {
                outputStream.close();
            }
        }
        catch (IOException e)
        {
            throw new MojoExecutionException(applicationFile.toString(), e);
        }
    }
}
