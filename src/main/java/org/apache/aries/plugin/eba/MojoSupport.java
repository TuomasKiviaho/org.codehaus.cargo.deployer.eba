package org.apache.aries.plugin.eba;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.AndArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilderException;
import org.apache.maven.shared.dependency.graph.DependencyNode;
import org.apache.maven.shared.dependency.graph.filter.ArtifactDependencyNodeFilter;
import org.apache.maven.shared.dependency.graph.filter.DependencyNodeFilter;
import org.apache.maven.shared.dependency.graph.traversal.CollectingDependencyNodeVisitor;
import org.apache.maven.shared.dependency.graph.traversal.DependencyNodeVisitor;
import org.apache.maven.shared.dependency.graph.traversal.FilteringDependencyNodeVisitor;

public abstract class MojoSupport extends AbstractMojo
{

    /**
     * @component
     */
    protected MavenProject project;

    /**
     * @component roleHint="default"
     */
    private DependencyGraphBuilder dependencyGraphBuilder;

    /**
     * @parameter property="eba.artifacts"
     */
    private String[] artifacts;

    /**
     * @parameter property="eba.scope" default-value="compile,runtime"
     */
    private String scopes;

    public MojoSupport()
    {
        super();
    }

    public static Manifest getManifest(File file) throws IOException
    {
        Manifest manifest = null;
        if (file.isDirectory())
        {
            File manifestFile = new File(file, JarFile.MANIFEST_NAME);
            if (manifestFile.exists())
            {
                InputStream inputStream = new FileInputStream(manifestFile);
                try
                {
                    manifest = new Manifest(inputStream);
                }
                finally
                {
                    inputStream.close();
                }
            }
        }
        else
        {
            JarFile jarFile = new JarFile(file);
            manifest = jarFile.getManifest();
        }
        return manifest;
    }

    protected abstract void doExecute(List<Artifact> artifacts) throws MojoExecutionException,
        MojoFailureException;

    public void execute() throws MojoExecutionException, MojoFailureException
    {
        final List<ArtifactFilter> excludeArtifactFilters =
            new ArrayList<ArtifactFilter>(this.artifacts.length);
        final List<ArtifactFilter> includeArtifactFilters =
            new ArrayList<ArtifactFilter>(this.artifacts.length);
        for (String artifact : this.artifacts)
        {
            boolean excludes = artifact.startsWith("!");
            List<String> expressions = new ArrayList<String>(4);
            Collections.addAll(expressions,
                (excludes ? artifact.substring(1) : artifact).split(":", -1));
            if (expressions.size() > 4)
            {
                throw new MojoFailureException(ArtifactFilter.class.getSimpleName() + " = "
                    + artifact);
            }
            if (expressions.size() == 1)
            {
                expressions.add(0, "*");
            }
            expressions.addAll(Collections.nCopies(4 - expressions.size(), "*"));
            final Pattern[] patterns = new Pattern[expressions.size()];
            for (int i = 0; i < patterns.length; i++)
            {
                String expression = expressions.get(i);
                String[] tokens = expression.split("\\*", -1);
                StringBuilder stringBuilder =
                    new StringBuilder(2 + expression.length() * 6 + tokens.length - 1);
                stringBuilder.append('^');
                for (int j = 0; j < tokens.length; j++)
                {
                    String token = tokens[j];
                    if (j > 0)
                    {
                        stringBuilder.append(".*");
                    }
                    if (!token.isEmpty())
                    {
                        String quote = Pattern.quote(token);
                        stringBuilder.append(quote);
                    }
                }
                stringBuilder.append('$');
                String regex = stringBuilder.toString();
                patterns[i] = Pattern.compile(regex);
            }
            ArtifactFilter artifactFilter = new ArtifactFilter()
            {

                public boolean include(Artifact artifact)
                {
                    CharSequence[] charSequences = new CharSequence[4];
                    charSequences[0] = artifact.getGroupId();
                    charSequences[1] = artifact.getArtifactId();
                    charSequences[2] = artifact.getType();
                    charSequences[3] = artifact.getClassifier();
                    boolean include = true;
                    for (int i = 0; i < patterns.length; i++)
                    {
                        Pattern pattern = patterns[i];
                        CharSequence charSequence = charSequences[i];
                        Matcher matcher =
                            pattern.matcher(charSequence == null ? "" : charSequences[i]);
                        if (!(include &= matcher.matches()))
                        {
                            break;
                        }
                    }
                    return include;
                }

            };
            List<ArtifactFilter> artifactFilters =
                excludes ? excludeArtifactFilters : includeArtifactFilters;
            artifactFilters.add(artifactFilter);
        }
        ArtifactFilter artifactFilter = new ArtifactFilter()
        {

            public boolean include(Artifact artifact)
            {
                boolean include = true;
                for (ArtifactFilter artifactFilter : excludeArtifactFilters)
                {
                    if (include &= !artifactFilter.include(artifact))
                    {
                        break;
                    }
                }
                if (include && !(include = includeArtifactFilters.isEmpty()))
                {
                    for (ArtifactFilter artifactFilter : includeArtifactFilters)
                    {
                        if (include = artifactFilter.include(artifact))
                        {
                            break;
                        }
                    }
                }
                return include;
            }

        };
        if (this.scopes != null)
        {
            final String[] scopes = this.scopes.split("\\,", -1);
            AndArtifactFilter andArtifactFilter = new AndArtifactFilter();
            ArtifactFilter scopeArtifactFilter = new ArtifactFilter()
            {

                public boolean include(Artifact artifact)
                {
                    boolean include = false;
                    for (int i = 0; i < scopes.length; i++)
                    {
                        String scope = artifact.getScope();
                        if (include |= scope == null || scopes[i].equals(scope))
                        {
                            break;
                        }

                    }
                    return include;
                }

            };
            andArtifactFilter.add(scopeArtifactFilter);
            andArtifactFilter.add(artifactFilter);
            artifactFilter = andArtifactFilter;
        }
        final Artifact projectArtifact = this.project.getArtifact();
        @SuppressWarnings("unchecked")
        List<Artifact> attachedArtifacts = this.project.getAttachedArtifacts();
        @SuppressWarnings("unchecked")
        Set<Artifact> projectArtifacts = this.project.getArtifacts();
        final List<Artifact> artifacts =
            new ArrayList<Artifact>(1 + attachedArtifacts.size() + projectArtifacts.size());
        if (projectArtifact != null && artifactFilter.include(projectArtifact))
        {
            artifacts.add(projectArtifact);
        }
        for (Artifact attachedArtifact : attachedArtifacts)
        {
            if (artifactFilter.include(attachedArtifact))
            {
                artifacts.add(attachedArtifact);
            }
        }
        try
        {
            final ArtifactFilter dependencyArtifactFilter = artifactFilter;
            ArtifactFilter orArtifactFilter = new ArtifactFilter()
            {

                public boolean include(Artifact artifact)
                {
                    return artifact.equals(projectArtifact)
                        || dependencyArtifactFilter.include(artifact);
                }

            };
            DependencyNode rootDependencyNode =
                this.dependencyGraphBuilder.buildDependencyGraph(this.project, null);
            CollectingDependencyNodeVisitor collectingDependencyNodeVisitor =
                new CollectingDependencyNodeVisitor();
            DependencyNodeFilter dependencyNodeFilter =
                new ArtifactDependencyNodeFilter(orArtifactFilter);
            DependencyNodeVisitor dependencyNodeVisitor =
                new FilteringDependencyNodeVisitor(collectingDependencyNodeVisitor,
                    dependencyNodeFilter);
            rootDependencyNode.accept(dependencyNodeVisitor);
            List<DependencyNode> dependencyNodes = collectingDependencyNodeVisitor.getNodes();
            Set<String> artifactIds = new HashSet<String>(dependencyNodes.size());
            for (DependencyNode dependencyNode : dependencyNodes)
            {
                Artifact dependencyArtifact = dependencyNode.getArtifact();
                String id = dependencyArtifact.getId();
                artifactIds.add(id);
            }
            for (Artifact artifact : projectArtifacts)
            {
                String id = artifact.getId();
                if (artifactIds.contains(id))
                {
                    artifacts.add(artifact);
                }
            }
        }
        catch (DependencyGraphBuilderException e)
        {
            throw new MojoExecutionException(e.getMessage(), e);
        }
        this.doExecute(artifacts);
    }

}
