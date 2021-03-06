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

package org.overturetool.vdmj.statements;

import org.overturetool.vdmj.definitions.Definition;
import org.overturetool.vdmj.definitions.ExplicitOperationDefinition;
import org.overturetool.vdmj.definitions.ImplicitOperationDefinition;
import org.overturetool.vdmj.expressions.Expression;
import org.overturetool.vdmj.expressions.ExpressionList;
import org.overturetool.vdmj.lex.LexNameToken;
import org.overturetool.vdmj.runtime.Context;
import org.overturetool.vdmj.runtime.ValueException;
import org.overturetool.vdmj.typechecker.Environment;
import org.overturetool.vdmj.typechecker.FlatEnvironment;
import org.overturetool.vdmj.typechecker.NameScope;
import org.overturetool.vdmj.types.OperationType;
import org.overturetool.vdmj.types.Type;
import org.overturetool.vdmj.types.TypeList;
import org.overturetool.vdmj.types.UnknownType;
import org.overturetool.vdmj.types.VoidType;
import org.overturetool.vdmj.util.Utils;
import org.overturetool.vdmj.values.Value;

public class PeriodicStatement extends Statement
{
	private static final long serialVersionUID = 1L;
	public final LexNameToken opname;
	public final ExpressionList args;

	public long[] values = new long[4];

	public PeriodicStatement(LexNameToken opname, ExpressionList args)
	{
		super(opname.location);
		this.opname = opname;
		this.args = args;
	}

	@Override
	public Type typeCheck(Environment env, NameScope scope, Type constraint)
	{
		if (args.size() != 4)
		{
			report(3287, "Periodic thread must have 4 argument(s)");
		}
		else
		{
			Environment functional = new FlatEnvironment(env, true);

			for (Expression arg: args)
			{
				Type type = arg.typeCheck(functional, null, scope, null);
				
				if (!type.isNumeric())
				{
					arg.report(3316, "Expecting number in periodic argument");
				}
			}
		}

		opname.setTypeQualifier(new TypeList());
		opname.location.hit();
		Definition opdef = env.findName(opname, NameScope.NAMES);

		if (opdef == null)
		{
			report(3228, opname + " is not in scope");
			return new UnknownType(location);
		}

		// Operation must be "() ==> ()"

		OperationType expected =
			new OperationType(location, new TypeList(), new VoidType(location));
		
		opdef = opdef.deref();

		if (opdef instanceof ExplicitOperationDefinition)
		{
			ExplicitOperationDefinition def = (ExplicitOperationDefinition)opdef;

			if (!def.type.equals(expected))
			{
				report(3229, opname + " should have no parameters or return type");
				detail("Actual", def.type);
			}
			else if (def.isPure())
			{
				report(3347, "Cannot have a pure operation as the body of a thread");
			}
		}
		else if (opdef instanceof ImplicitOperationDefinition)
		{
			ImplicitOperationDefinition def = (ImplicitOperationDefinition)opdef;

			if (def.body == null)
			{
				report(3230, opname + " is implicit");
			}

			if (!def.type.equals(expected))
			{
				report(3231, opname + " should have no parameters or return type");
				detail("Actual", def.type);
			}
			else if (def.isPure())
			{
				report(3347, "Cannot have a pure operation as the body of a thread");
			}
		}
		else
		{
			report(3232, opname + " is not an operation name");
		}

		return new VoidType(location);
	}

	@Override
	public Value eval(Context ctxt)
	{
		int i = 0;
		
		for (Expression arg: args)
		{
			Value argval = null;
			
			try
			{
				arg.location.hit();
				argval = arg.eval(ctxt);
				values[i] = argval.intValue(ctxt);

				if (values[i] < 0)
				{
					abort(4157, "Expecting +ive integer in periodic argument " + (i+1) + ", was " + values[i], ctxt);
				}
			}
			catch (ValueException e)
			{
				abort(4157, "Expecting +ive integer in periodic argument " + (i+1) + ", was " + argval, ctxt);
			}

			i++;
		}

		if (values[0] == 0)
		{
			abort(4158, "Period argument must be non-zero, was " + values[0], ctxt);
		}

		if (args.size() == 4)
		{
			if (values[2] >= values[0])
			{
				abort(4159, "Delay argument (" + values[2] + ") must be less than the period (" + values[0] + ")", ctxt);
			}
		}
		
		return null;	// Not actually used - see StartStatement
	}

	@Override
	public String kind()
	{
		return "periodic statement";
	}

	@Override
	public String toString()
	{
		return "periodic(" + Utils.listToString(args) + ")(" + opname + ")";
	}
}
