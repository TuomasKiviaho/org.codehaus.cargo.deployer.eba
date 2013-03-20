package org.apache.aries.plugin.eba;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.util.List;

import org.apache.felix.bundlerepository.Resource;
import org.apache.felix.bundlerepository.impl.DataModelHelperImpl;
import org.apache.felix.bundlerepository.impl.FileHeaders;
import org.apache.felix.bundlerepository.impl.RepositoryImpl;
import org.apache.felix.bundlerepository.impl.ResourceImpl;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * @goal repository
 * @phase prepare-package
 * @requiresProject true
 * @requiresDependencyResolution test
 */
public class RepositoryMojo extends MojoSupport
{

    private static final String REPOSITORY_NAME = "repository.xml";

    /**
     * @parameter default-value="${settings.localRepository}"
     * @required
     */
    private File outputDirectory;

    public RepositoryMojo()
    {
        super();
    }

    @Override
    protected void doExecute(List<Artifact> artifacts) throws MojoExecutionException,
        MojoFailureException
    {
        DataModelHelperImpl dataModelHelper = new DataModelHelperImpl();
        RepositoryImpl repository = new RepositoryImpl();
        for (Artifact artifact : artifacts)
        {
            File file = artifact.getFile();
            if (file != null)
            {
                ResourceImpl resource;
                try
                {
                    FileHeaders headers = new FileHeaders(file);
                    resource = dataModelHelper.createResource(headers);
                }
                catch (FileNotFoundException e)
                {
                    resource = null;
                }
                catch (IOException e)
                {
                    throw new MojoExecutionException(artifact.toString(), e);
                }
                if (resource != null)
                {
                    if (!file.isDirectory())
                    {
                        String size = Long.toString(file.length());
                        resource.put(Resource.SIZE, size, null);
                    }
                    URI uri = file.toURI();
                    resource.put(Resource.URI, "reference:" + uri.toString(), null);
                    repository.addResource(resource);
                }
            }
        }
        File file = new File(this.outputDirectory, REPOSITORY_NAME);
        File parentFile = file.getParentFile();
        parentFile.mkdirs();
        try
        {
            Writer writer = new FileWriter(file);
            try
            {
                dataModelHelper.writeRepository(repository, writer);
            }
            finally
            {
                writer.close();
            }
        }
        catch (IOException e)
        {
            throw new MojoExecutionException(file.toString(), e);
        }
    }

}
