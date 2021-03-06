/*******************************************************************************
 *
 *	Copyright (C) 2008, 2009 Fujitsu Services Ltd.
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
import org.overturetool.vdmj.scheduler.SystemClock;
import org.overturetool.vdmj.typechecker.Environment;
import org.overturetool.vdmj.typechecker.NameScope;
import org.overturetool.vdmj.types.NaturalType;
import org.overturetool.vdmj.types.Type;
import org.overturetool.vdmj.types.TypeList;
import org.overturetool.vdmj.values.NaturalValue;
import org.overturetool.vdmj.values.Value;

public class TimeExpression extends Expression
{
	private static final long serialVersionUID = 1L;

	public TimeExpression(LexLocation location)
	{
		super(location);
	}

	@Override
	public Value eval(Context ctxt)
	{
		location.hit();

		try
        {
	        return new NaturalValue(SystemClock.getWallTime());
        }
        catch (Exception e)
        {
        	return abort(4145, "Time: " + e.getMessage(), ctxt);
        }
	}

	@Override
	public String kind()
	{
		return "time";
	}

	@Override
	public String toString()
	{
		return "time";
	}

	@Override
	public Type typeCheck(Environment env, TypeList qualifiers, NameScope scope, Type constraint)
	{
		return checkConstraint(constraint, new NaturalType(location));
	}
}
