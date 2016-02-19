/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.infrastructure.process.rules;

import static java.lang.System.getProperties;
import static java.lang.System.getProperty;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Prober;
import org.mule.test.infrastructure.process.AppDeploymentProbe;
import org.mule.test.infrastructure.process.MuleProcessController;
import org.mule.test.infrastructure.process.MuleStatusProbe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.FilenameUtils;
import org.junit.rules.ExternalResource;

/**
 * This is a JUnit rule to start and stop Mule Runtime during tests. Usage:
 * <p/>
 * <pre>
 * public static class MuleStandaloneIntegrationTests {
 *  &#064;Rule
 *  public MuleStandalone standalone = new MuleStandalone(&quot;/path/to/mule/home&quot;);
 *
 *  &#064;Test
 *  public void integrationTest() throws IOException
 *  {
 *      MuleProcessController mule = standalone.getMule();
 *      assertThat(mule.isRunning(), is(true));
 *  }
 * }
 * </pre>
 */
public class MuleAppDeploy extends ExternalResource
{

    private final MuleProcessController mule;
    private String appPath;
    private String[] args;

    private Prober prober = new PollingProber(Integer.parseInt(getProperty("mule.timeout", "60000")), 100);


    public MuleAppDeploy(String muleHome, String appPath)
    {
        mule = new MuleProcessController(muleHome);
        this.appPath = appPath;
    }

    public MuleAppDeploy(String muleHome, String appPath, String... args)
    {
        this(muleHome, appPath);
        this.args = addSystemPropertiesToArgs(args);
    }

    @Override
    protected void before() throws Throwable
    {
        mule.deploy(appPath);
        mule.start(args);
        prober.check(AppDeploymentProbe.isDeployed(mule, getAppName(appPath)));
    }

    @Override
    protected void after()
    {
        mule.stop();
        prober.check(MuleStatusProbe.isNotRunning(mule));
    }

    private String[] addSystemPropertiesToArgs(String[] args)
    {
        List<String> argsFromSystemProperties = mapSystemPropertiesAsMuleArgs(getProperties());

        ArrayList<String> argsAsList = new ArrayList<String>(Arrays.asList(args));
        argsAsList.addAll(argsFromSystemProperties);

        return (String[]) argsAsList.toArray();
    }

    private List<String> mapSystemPropertiesAsMuleArgs(Properties systemProperties)
    {
        List<String> muleArgs = new ArrayList<>();

        for (String propertyName : systemProperties.stringPropertyNames())
        {
            String muleArg = "-M-D" + propertyName + "=" + systemProperties.getProperty(propertyName);
            muleArgs.add(muleArg);
        }
        return muleArgs;
    }

    private String getAppName(String appPath)
    {
        return FilenameUtils.getBaseName(FilenameUtils.getFullPathNoEndSeparator(appPath));
    }

    public MuleProcessController getMule()
    {
        return mule;
    }

}
