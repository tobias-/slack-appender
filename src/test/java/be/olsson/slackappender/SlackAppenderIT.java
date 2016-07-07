package be.olsson.slackappender;

import static org.junit.Assert.assertNotNull;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("ALL")
public class SlackAppenderIT {
    public static final String SLACK_WEBHOOK = "SLACK_WEBHOOK";

    @Before
    public void setup() {
	ConfigurationBuilder<BuiltConfiguration> builder = ConfigurationBuilderFactory.newConfigurationBuilder();
	String webhookUrl = System.getProperty(SLACK_WEBHOOK, System.getenv(SLACK_WEBHOOK));
	assertNotNull(SLACK_WEBHOOK + " MUST NOT be null", webhookUrl);
	AppenderComponentBuilder appenderComponentBuilder = builder.newAppender("Slack", "SlackAppender");
	appenderComponentBuilder.addAttribute("webhookUrl", webhookUrl);
	appenderComponentBuilder.add(builder.newLayout("PatternLayout").
		addAttribute("pattern", "%-5p - [%t] %-26.26c{1}"));
	builder.add(appenderComponentBuilder);
	builder.add(builder.newRootLogger(Level.INFO).add(builder.newAppenderRef("Slack")));
	Configurator.initialize(builder.build());
    }


    @Test
    public void sendInfoWithoutStack() throws InterruptedException {
	LogManager.getLogger(getClass()).warn("Test warning");
	Thread.sleep(1000);
    }

    @Test
    public void sendInfo() throws InterruptedException {
	LogManager.getLogger(getClass()).warn("Test warning", new Throwable("This is a test exception"));
	Thread.sleep(1000);
    }

    @Test
    public void sendInfoShorted() throws InterruptedException {
	SlackAppender slack = ((LoggerContext) LogManager.getContext(false)).getConfiguration().getAppender("Slack");
	slack.setPackagesToMute("org.junit.,sun.reflect");
	LogManager.getLogger(getClass()).warn("Test warning", new Throwable("This is a test exception"));
	Thread.sleep(1000);
    }

}
