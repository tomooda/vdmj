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

package org.overturetool.vdmj.expressions;

import org.overturetool.vdmj.lex.LexLocation;
import org.overturetool.vdmj.runtime.Context;
import org.overturetool.vdmj.runtime.ValueException;
import org.overturetool.vdmj.typechecker.Environment;
import org.overturetool.vdmj.typechecker.NameScope;
import org.overturetool.vdmj.types.SetType;
import org.overturetool.vdmj.types.Type;
import org.overturetool.vdmj.types.TypeList;
import org.overturetool.vdmj.types.UnknownType;
import org.overturetool.vdmj.values.SetValue;
import org.overturetool.vdmj.values.Value;
import org.overturetool.vdmj.values.ValueSet;

public class DistUnionExpression extends UnaryExpression
{
	private static final long serialVersionUID = 1L;

	public DistUnionExpression(LexLocation location, Expression exp)
	{
		super(location, exp);
	}

	@Override
	public String toString()
	{
		return "(dunion " + exp + ")";
	}

	@Override
	public Type typeCheck(Environment env, TypeList qualifiers, NameScope scope, Type constraint)
	{
		SetType setType = null;
		
		if (constraint != null)
		{
			setType = new SetType(location, constraint);
		}

		Type type = exp.typeCheck(env, null, scope, setType);

		if (type.isSet())
		{
			SetType set = type.getSet();

			if (set.setof.isSet())
			{
				return set.setof;
			}
		}

		report(3078, "dunion argument is not a set of sets");
		return new SetType(location, new UnknownType(location));
	}

	@Override
	public Value eval(Context ctxt)
	{
		breakpoint.check(location, ctxt);

		try
		{
    		ValueSet setset = exp.eval(ctxt).setValue(ctxt);
    		ValueSet result = new ValueSet();

    		for (Value v: setset)
    		{
   				result.addAll(v.setValue(ctxt));
    		}

    		return new SetValue(result);
        }
        catch (ValueException e)
        {
        	return abort(e);
        }
	}

	@Override
	public String kind()
	{
		return "dunion";
	}
}
