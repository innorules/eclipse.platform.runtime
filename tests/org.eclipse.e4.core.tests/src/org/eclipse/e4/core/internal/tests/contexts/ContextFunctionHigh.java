package org.eclipse.e4.core.internal.tests.contexts;

import org.eclipse.e4.core.contexts.ContextFunction;
import org.eclipse.e4.core.contexts.IEclipseContext;

public class ContextFunctionHigh extends ContextFunction {
	@Override
	public Object compute(IEclipseContext context) {
		return "High";
	}
}
