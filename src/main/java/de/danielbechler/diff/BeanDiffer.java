package de.danielbechler.diff;

import de.danielbechler.diff.accessor.*;
import de.danielbechler.diff.introspect.*;
import de.danielbechler.diff.node.*;
import de.danielbechler.util.*;

/** @author Daniel Bechler */
final class BeanDiffer extends AbstractDiffer
{
	private Introspector introspector = new StandardIntrospector();

	BeanDiffer()
	{
		setDelegate(new DelegatingObjectDiffer(this, null, null));
	}

	BeanDiffer(final ObjectDiffer delegate)
	{
		super(delegate);
	}

	Node compare(final Object working, final Object base)
	{
		// Root call requires an existing working instance
		Assert.notNull(working, "working");

		// Comparison of different types is not (yet) supported
		Assert.equalTypesOrNull(working, base);

		return compare(Node.ROOT, Instances.of(new RootAccessor(), working, base));
	}

	public Node compare(final Node parentNode, final Instances instances)
	{
		final Node node = new DefaultNode(parentNode, instances.getSourceAccessor());

		if (getDelegate().isIgnored(node, instances))
		{
			node.setState(Node.State.IGNORED);
			return node;
		}

		if (instances.getType() == null)
		{
			node.setState(Node.State.UNTOUCHED);
			return node;
		}

		return compareBean(parentNode, instances);
	}

	private Node compareBean(final Node parentNode, final Instances instances)
	{
		final Node difference = new DefaultNode(parentNode, instances.getSourceAccessor());
		if (instances.hasBeenAdded())
		{
			difference.setState(Node.State.ADDED);
		}
		else if (instances.hasBeenRemoved())
		{
			difference.setState(Node.State.REMOVED);
		}
		else if (instances.areSame())
		{
			difference.setState(Node.State.UNTOUCHED);
		}
		else
		{
			if (getDelegate().isEqualsOnly(parentNode, instances))
			{
				compareWithEquals(difference, instances);
			}
			else
			{
				compareProperties(difference, instances);
			}
		}
		return difference;
	}

	private static void compareWithEquals(final Node parentNode, final Instances instances)
	{
		if (!instances.areEqual())
		{
			parentNode.setState(Node.State.CHANGED);
		}
	}

	private void compareProperties(final Node parentNode, final Instances instances)
	{
		for (final Accessor accessor : introspect(instances.getType()))
		{
			final Node child = getDelegate().compare(parentNode, instances.access(accessor));
			if (child.hasChanges())
			{
				parentNode.setState(Node.State.CHANGED);
				parentNode.addChild(child);
			}
			else if (getConfiguration().isReturnUnchangedNodes())
			{
				parentNode.addChild(child);
			}
		}
	}

	private Iterable<Accessor> introspect(final Class<?> type)
	{
		return introspector.introspect(type);
	}

	void setIntrospector(final Introspector introspector)
	{
		Assert.notNull(introspector, "introspector");
		this.introspector = introspector;
	}
}