package org.eclipse.e4.core.internal.tests.nls;

import java.text.MessageFormat;

import javax.annotation.PostConstruct;

/**
 * Load messages from a relative positioned resource bundle (./SimpleMessages.properties)
 */
public class SimpleMessages {

	//message as is
	public String message;

	//message as is with underscore
	public String message_one;

	//message as is camel cased
	public String messageOne;

	//message with underscore transformed to . separated properties key
	public String message_two;

	//camel cased message transformed to . separated properties key
	public String messageThree;

	//message with placeholder
	public String messageFour;

	@PostConstruct
	public void format() {
		messageFour = MessageFormat.format(messageFour, "Tom"); //$NON-NLS-1$
	}
}
