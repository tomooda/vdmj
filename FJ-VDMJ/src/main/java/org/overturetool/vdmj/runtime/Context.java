/*******************************************************************************
 *
 *	Copyright (C) 2008 Fujitsu Services Ltd.
 *
 *	Author: Nick Battle
 *
 *	This file is part of VDMJ.
 *
 *	VDMJ is free software: you can redistribute it and/or modify
 *	it under the terms of the GNU General Public License as published by
 *	the Free Software Foundation, either version 3 of the License, or
 *	(at your option) any later version.
 *
 *	VDMJ is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *	GNU General Public License for more details.
 *
 *	You should have received a copy of the GNU General Public License
 *	along with VDMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 ******************************************************************************/

package org.overturetool.vdmj.runtime;

import java.io.PrintWriter;
import java.util.HashMap;

import org.overturetool.vdmj.debug.DBGPReader;
import org.overturetool.vdmj.lex.LexLocation;
import org.overturetool.vdmj.lex.LexNameToken;
import org.overturetool.vdmj.values.CPUValue;
import org.overturetool.vdmj.values.NameValuePair;
import org.overturetool.vdmj.values.NameValuePairList;
import org.overturetool.vdmj.values.ObjectValue;
import org.overturetool.vdmj.values.OperationValue;
import org.overturetool.vdmj.values.Value;


/**
 * A class to hold runtime name/value context information.
 */

@SuppressWarnings("serial")
public class Context extends HashMap<LexNameToken, Value>
{
	/** The location of the context. */
	public final LexLocation location;
	/** The name of the location. */
	public final String title;
	/** A link to a lower level context, if present. */
	public final Context outer;
	/** The thread state associated with this context. */
	public ThreadState threadState = null;

	/** Non-zero if this is a pre or postcondition call. */
	public int prepost = 0;
	/** Set to the error message if prepost is set. */
	public String prepostMsg = null;
	/** Set to the operation being guarded, if any. */
	public OperationValue guardOp = null;

	/**
	 * Create a context at the given location.
	 *
	 * @param location
	 * @param title
	 * @param outer
	 */

	public Context(LexLocation location, String title, Context outer)
	{
		this.location = location;
		this.outer = outer;
		this.title = title;

		if (outer != null)
		{
			this.threadState = outer.threadState;
		}
	}

	/**
	 * Set the current thread state. Note this must be called from the thread
	 * where the context will run, which may not be where the thread is created.
	 * And it must be called before any context chaining is performed.
	 * @param cpu TODO
	 */

	public void setThreadState(DBGPReader dbgp, CPUValue cpu)
	{
		threadState = new ThreadState(dbgp, cpu);
	}

	/**
	 * Find the outermost context from this one.
	 *
	 * @return The outermost context.
	 */

	public Context getGlobal()
	{
		Context op = this;

		while (op.outer != null)
		{
			op = op.outer;
		}

		return op;
	}

	/**
	 * Find the nearest RootContext in the context chain.
	 */

	public RootContext getRoot()
	{
		assert outer != null : "Root context is wrong type";
		return outer.getRoot();		// RootContext overrides this!
	}

	/**
	 * Make a deep copy of the context, using Value.deepCopy.
	 */

	public Context deepCopy()
	{
		Context below = null;

		if (outer != null)
		{
			below = outer.deepCopy();
		}

		Context result = new Context(location, title, below);

		for (LexNameToken var: keySet())
		{
			Value v = get(var);
			result.put(var, v.deepCopy());
		}

		return result;
	}

	/**
	 * Add a list of name/value pairs to this context.
	 *
	 * @param nvl A list of name/value pairs.
	 */

	public void putList(NameValuePairList nvl)
	{
		for (NameValuePair nv: nvl)
		{
			put(nv.name, nv.value);
		}
	}

	public void putNew(NameValuePair nvp)
	{
		if (get(nvp.name) == null)
		{
			put(nvp.name, nvp.value);
		}
	}

	public void putAllNew(NameValuePairList list)
	{
		for (NameValuePair nvp: list)
		{
			putNew(nvp);
		}
	}

	/**
	 * Get a name, taking type overloading into account. If we use the
	 * superclass method, different names are considered different,
	 * because the map is driven by the names' hashCodes. The equals
	 * method of LexNameToken makes a TypeComparator check, which is
	 * what we need. But we try a simple super.get() first.
	 *
	 * TODO Slow though.
	 */

	@Override
	public Value get(Object name)
	{
		Value rv = super.get(name);

		if (rv == null)
		{
    		for (LexNameToken var: keySet())
    		{
    			if (var.equals(name))
    			{
    				rv = super.get(var);
    				break;
    			}
    		}
		}

		return rv;
	}

	/**
	 * Get all visible names from this Context, with more visible
	 * values overriding those below.
	 *
	 * @return	A new Context with all visible names.
	 */

	public Context getVisibleVariables()
	{
		Context visible = new Context(location, title, null);

		if (outer != null)
		{
			visible.putAll(outer.getVisibleVariables());
		}

		visible.putAll(this);	// Overriding anything below here
		return visible;
	}

	/**
	 * Get the value for a given name. This searches outer contexts, if
	 * any are present.
	 *
	 * @param name The name to look for.
	 * @return The value of the name, or null.
	 */

	public Value check(LexNameToken name)
	{
		Value v = get(name);

		if (v == null)
		{
			if (outer != null)
			{
				return outer.check(name);
			}
		}

		return v;
	}

	/**
	 * Locate the Context in a chain that contains a name, if any.
	 */

	public Context locate(LexNameToken name)
	{
		Value v = get(name);

		if (v == null)
		{
			if (outer != null)
			{
				return outer.locate(name);
			}
			else
			{
				return null;
			}
		}
		else
		{
			return this;
		}
	}

	/**
	 * Return the value of a name, else fail. If the name is not present, a
	 * {@link ContextException} is thrown.
	 *
	 * @param name The name to look for.
	 * @return The value of the name.
	 */

	public Value lookup(LexNameToken name)
	{
		Value v = check(name);

		if (v == null)
		{
			name.abort(4034, "Name '" + name + "' not in scope", this);
		}

		return v;
	}

	@Override
	public String toString()
	{
		return format("", this);
	}

	protected String format(String indent, Context what)
	{
		StringBuilder sb = new StringBuilder();

		for (LexNameToken name: what.keySet())
		{
			sb.append(indent + name + " = " +
				what.get(name).toShortString(100) + "\n");
		}

		return sb.toString();
	}

	public void printStackTrace(PrintWriter out, boolean variables)
	{
		if (outer == null)		// Don't expand initial context
		{
			out.println("In context of " + title);
		}
		else
		{
			if (variables)
			{
				out.print(this.format("\t", this));
			}

			out.println("In context of " + title + " " + location);
			outer.printStackTrace(out, variables);
		}
	}

	public int getDepth()
	{
		return outer == null ? 0 : outer.getDepth();	// NB only roots count
	}

	public Context getFrame(int depth)
	{
		return outer == null ? this : outer.getFrame(depth);
	}

	public ObjectValue getSelf()
	{
		return outer == null ? null : outer.getSelf();
	}

	public void setPrepost(int prepost, String prepostMsg)
	{
		this.prepost = prepost;
		this.prepostMsg = prepostMsg;
	}
}
