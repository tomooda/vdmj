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
import org.overturetool.vdmj.types.NaturalOneType;
import org.overturetool.vdmj.types.SetType;
import org.overturetool.vdmj.types.Type;
import org.overturetool.vdmj.types.TypeList;
import org.overturetool.vdmj.values.NaturalOneValue;
import org.overturetool.vdmj.values.SetValue;
import org.overturetool.vdmj.values.Value;
import org.overturetool.vdmj.values.ValueList;
import org.overturetool.vdmj.values.ValueSet;

public class IndicesExpression extends UnaryExpression
{
	private static final long serialVersionUID = 1L;

	public IndicesExpression(LexLocation location, Expression exp)
	{
		super(location, exp);
	}

	@Override
	public String toString()
	{
		return "(inds " + exp + ")";
	}

	@Override
	public Type typeCheck(Environment env, TypeList qualifiers, NameScope scope, Type constraint)
	{
		Type etype = exp.typeCheck(env, null, scope, null);

		if (!etype.isSeq())
		{
			report(3109, "Argument to 'inds' is not a sequence");
			detail("Actual type", etype);
		}

		return checkConstraint(constraint, new SetType(location, new NaturalOneType(location)));
	}

	@Override
	public Value eval(Context ctxt)
	{
		breakpoint.check(location, ctxt);

		try
		{
    		ValueList seq = exp.eval(ctxt).seqValue(ctxt);
    		ValueSet result = new ValueSet();

    		for (int i=1; i<= seq.size(); i++)
    		{
    			result.add(new NaturalOneValue(i));
    		}

    		return new SetValue(result);
        }
        catch (ValueException e)
        {
        	return abort(e);
        }
		catch (Exception e)
		{
			return abort(4065, e.getMessage(), ctxt);
		}
	}

	@Override
	public String kind()
	{
		return "inds";
	}
}
