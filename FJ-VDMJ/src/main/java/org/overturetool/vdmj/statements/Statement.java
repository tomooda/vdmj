/*******************************************************************************
 *
 *	Copyright (c) 2008 Fujitsu Services Ltd.
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

package org.overturetool.vdmj.statements;

import java.io.Serializable;

import org.overturetool.vdmj.definitions.Definition;
import org.overturetool.vdmj.definitions.ExplicitOperationDefinition;
import org.overturetool.vdmj.definitions.ImplicitOperationDefinition;
import org.overturetool.vdmj.definitions.InheritedDefinition;
import org.overturetool.vdmj.expressions.Expression;
import org.overturetool.vdmj.lex.LexLocation;
import org.overturetool.vdmj.pog.POContextStack;
import org.overturetool.vdmj.pog.ProofObligationList;
import org.overturetool.vdmj.runtime.Breakpoint;
import org.overturetool.vdmj.runtime.Context;
import org.overturetool.vdmj.runtime.ContextException;
import org.overturetool.vdmj.runtime.ExceptionHandler;
import org.overturetool.vdmj.runtime.ValueException;
import org.overturetool.vdmj.typechecker.Environment;
import org.overturetool.vdmj.typechecker.NameScope;
import org.overturetool.vdmj.typechecker.TypeChecker;
import org.overturetool.vdmj.typechecker.TypeComparator;
import org.overturetool.vdmj.types.Type;
import org.overturetool.vdmj.types.TypeSet;
import org.overturetool.vdmj.types.VoidType;
import org.overturetool.vdmj.values.Value;

/**
 * The parent class of all statements.
 */

public abstract class Statement implements Serializable
{
	private static final long serialVersionUID = 1L;

	/** The location of the statement. */
	public final LexLocation location;

	/** The statement's breakpoint, if any. */
	public Breakpoint breakpoint;

	/**
	 * Create a statement at the given location.
	 * @param location
	 */

	public Statement(LexLocation location)
	{
		this.location = location;
		this.breakpoint = new Breakpoint(location);

		location.executable(true);
	}

	@Override
	abstract public String toString();

	/** A string name of the statement type, (eg "if"). */
	abstract public String kind();

	/** Type check the statement and return its type. 
	 * @param constraint TODO*/ 
	abstract public Type typeCheck(Environment env, NameScope scope, Type constraint);

	/**
	 * Check that a return type meets a constraint. This is used in various statements
	 * to verify that a constraining type is met (see typeCheck methods).
	 */
	protected Type checkReturnType(Type constraint, Type actual)
	{
		if (constraint != null && !(actual instanceof VoidType) && !(actual.isUnknown()))
		{
			if (actual.hasVoid() && !(constraint instanceof VoidType))
			{
				report(3328, "Statement may return void value");
				detail2("Actual", actual, "Expected", constraint);
			}
			else if (!TypeComparator.compatible(constraint, actual))
			{
				report(3327, "Value is not of the right type");
				detail2("Actual", actual, "Expected", constraint);
			}
		}

		return actual;
	}

	/**
	 * Get a list of proof obligations from the statement.
	 *
	 * @param ctxt The call context.
	 * @return The list of proof obligations.
	 */

	public ProofObligationList getProofObligations(POContextStack ctxt)
	{
		return new ProofObligationList();
	}

	/**
	 * Whether the statement probably produces side effects. This is used during
	 * PO generation for operations. The method will produce false positives
	 * (saying that side effects are produced, when they are not), but no false
	 * negatives.
	 */

	public boolean hasSideEffects()
	{
		return true;
	}

	/**
	 * Return a list of exit types which can be thrown by the statement.
	 * @return A possibly empty list of exit types.
	 */

	public TypeSet exitCheck()
	{
		return new TypeSet();
	}

	/**
	 * Find a statement starting on the given line. Single statements just
	 * compare their location to lineno, but block statements and statements
	 * with sub-statements iterate over their branches.
	 *
	 * @param lineno The line number to locate.
	 * @return A statement starting on the line, or null.
	 */

	public Statement findStatement(int lineno)
	{
		return (location.startLine == lineno) ? this : null;
	}

	public Expression findExpression(int lineno)
	{
		return null;
	}

	/** Evaluate the statement in the context given. */
	abstract public Value eval(Context ctxt);

	/**
	 * @see org.overturetool.vdmj.definitions.Definition#report
	 */

	public void report(int number, String msg)
	{
		TypeChecker.report(number, msg, location);
	}

	/**
	 * @see org.overturetool.vdmj.definitions.Definition#warning
	 */

	public void warning(int number, String msg)
	{
		TypeChecker.warning(number, msg, location);
	}

	/**
	 * @see org.overturetool.vdmj.definitions.Definition#abort
	 */

	public Value abort(int number, String msg, Context ctxt)
	{
		ExceptionHandler.handle(new ContextException(number, msg, location, ctxt));
		return null;
	}

	/**
	 * @see org.overturetool.vdmj.definitions.Definition#abort
	 */

	public Value abort(ValueException ve)
	{
		ExceptionHandler.handle(new ContextException(ve, location));
		return null;
	}

	/**
	 * @see org.overturetool.vdmj.definitions.Definition#detail
	 */

	public void detail(String tag, Object obj)
	{
		TypeChecker.detail(tag, obj);
	}

	/**
	 * @see org.overturetool.vdmj.definitions.Definition#detail2
	 */

	public void detail2(String tag1, Object obj1, String tag2, Object obj2)
	{
		TypeChecker.detail2(tag1, obj1, tag2, obj2);
	}
	
	/**
	 * Test whether a definition is a class constructor.
	 */
	public static boolean isConstructor(Definition def)
	{
		if (def instanceof ExplicitOperationDefinition)
		{
			ExplicitOperationDefinition op = (ExplicitOperationDefinition)def;
			return op.isConstructor;
		}
		else if (def instanceof ImplicitOperationDefinition)
		{
			ImplicitOperationDefinition op = (ImplicitOperationDefinition)def;
			return op.isConstructor;
		}
		else if (def instanceof InheritedDefinition)
		{
			InheritedDefinition op = (InheritedDefinition)def;
			return isConstructor(op.superdef);
		}
		
		return false;
	}

	/**
	 * Test whether the calling environment indicates that we are within a constructor.
	 */
	public static boolean inConstructor(Environment env)
	{
		Definition encl = env.getEnclosingDefinition();
	
		if (encl != null)
		{
			return isConstructor(encl);
		}
		
		return false;
	}
}
