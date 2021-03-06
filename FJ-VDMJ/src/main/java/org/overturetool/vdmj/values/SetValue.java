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

package org.overturetool.vdmj.values;

import java.util.List;

import org.overturetool.vdmj.runtime.Context;
import org.overturetool.vdmj.runtime.ValueException;
import org.overturetool.vdmj.types.SetType;
import org.overturetool.vdmj.types.Type;
import org.overturetool.vdmj.types.TypeSet;


public class SetValue extends Value
{
	private static final long serialVersionUID = 1L;
	public final ValueSet values;

	public SetValue()
	{
		this.values = new ValueSet();
	}

	public SetValue(ValueSet values)
	{
		// We arrange that VDMJ set values usually have sorted contents.
		// This guarantees deterministic behaviour in places that would
		// otherwise be variable.

		this(values, true);
	}

	public SetValue(ValueSet values, boolean sort)
	{
		if (sort)
		{
			values.sort();
		}

		this.values = values;
	}

	@Override
	public ValueSet setValue(Context ctxt)
	{
		return values;
	}

	@Override
	public Value getUpdatable(ValueListenerList listeners)
	{
		ValueSet nset = new ValueSet();

		for (Value k: values)
		{
			Value v = k.getUpdatable(listeners);
			nset.add(v);
		}

		return UpdatableValue.factory(new SetValue(nset), listeners);
	}

	@Override
	public Value getConstant()
	{
		ValueSet nset = new ValueSet();

		for (Value k: values)
		{
			Value v = k.getConstant();
			nset.add(v);
		}

		return new SetValue(nset);
	}

	@Override
	public boolean equals(Object other)
	{
		if (other instanceof Value)
		{
			Value val = ((Value)other).deref();

    		if (val instanceof SetValue)
    		{
    			SetValue ot = (SetValue)val;
    			return values.equals(ot.values);
    		}
		}

		return false;
	}

	@Override
	public String toString()
	{
		return values.toString();
	}

	@Override
	public int hashCode()
	{
		return values.hashCode();
	}

	public ValueList permutedSets()
	{
		List<ValueSet> psets = values.permutedSets();
		ValueList rs = new ValueList(psets.size());

		for (ValueSet v: psets)
		{
			rs.add(new SetValue(v, false));		// NB not re-sorted!
		}

		return rs;
	}

	@Override
	public String kind()
	{
		return "set";
	}

	@Override
	protected Value convertValueTo(Type to, Context ctxt, TypeSet done) throws ValueException
	{
		if (to instanceof SetType)
		{
			SetType setto = (SetType)to;
			ValueSet ns = new ValueSet();

			for (Value v: values)
			{
				ns.add(v.convertValueTo(setto.setof, ctxt));
			}

			return new SetValue(ns);
		}
		else
		{
			return super.convertValueTo(to, ctxt, done);
		}
	}

	@Override
	public Object clone()
	{
		return new SetValue((ValueSet)values.clone());
	}
}
