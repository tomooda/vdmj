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

import java.util.List;

import org.overturetool.vdmj.definitions.Definition;
import org.overturetool.vdmj.definitions.MultiBindListDefinition;
import org.overturetool.vdmj.lex.LexLocation;
import org.overturetool.vdmj.lex.LexNameList;
import org.overturetool.vdmj.patterns.MultipleBind;
import org.overturetool.vdmj.patterns.MultipleTypeBind;
import org.overturetool.vdmj.patterns.Pattern;
import org.overturetool.vdmj.pog.FiniteMapObligation;
import org.overturetool.vdmj.pog.MapSetOfCompatibleObligation;
import org.overturetool.vdmj.pog.POForAllPredicateContext;
import org.overturetool.vdmj.pog.POForAllContext;
import org.overturetool.vdmj.pog.POContextStack;
import org.overturetool.vdmj.pog.ProofObligationList;
import org.overturetool.vdmj.runtime.Context;
import org.overturetool.vdmj.runtime.ValueException;
import org.overturetool.vdmj.typechecker.Environment;
import org.overturetool.vdmj.typechecker.FlatCheckedEnvironment;
import org.overturetool.vdmj.typechecker.NameScope;
import org.overturetool.vdmj.types.BooleanType;
import org.overturetool.vdmj.types.Type;
import org.overturetool.vdmj.types.TypeList;
import org.overturetool.vdmj.util.Utils;
import org.overturetool.vdmj.values.MapValue;
import org.overturetool.vdmj.values.NameValuePair;
import org.overturetool.vdmj.values.NameValuePairList;
import org.overturetool.vdmj.values.Quantifier;
import org.overturetool.vdmj.values.QuantifierList;
import org.overturetool.vdmj.values.Value;
import org.overturetool.vdmj.values.ValueList;
import org.overturetool.vdmj.values.ValueMap;


public class MapCompExpression extends MapExpression
{
	private static final long serialVersionUID = 1L;
	public final MapletExpression first;
	public final List<MultipleBind> bindings;
	public final Expression predicate;
	private Type maptype;

	public MapCompExpression(LexLocation start,
		MapletExpression first, List<MultipleBind> bindings,
		Expression predicate)
	{
		super(start);
		this.first = first;
		this.bindings = bindings;
		this.predicate = predicate;
	}

	@Override
	public String toString()
	{
		return "{" + first + " | " + Utils.listToString(bindings) +
			(predicate == null ? "}" : " & " + predicate + "}");
	}

	@Override
	public Type typeCheck(Environment base, TypeList qualifiers, NameScope scope, Type constraint)
	{
		Definition def = new MultiBindListDefinition(location, bindings);
		def.typeCheck(base, scope);
		Environment local = new FlatCheckedEnvironment(def, base, scope);

		if (predicate != null &&
			!predicate.typeCheck(local, null, scope, new BooleanType(location)).isType(BooleanType.class))
		{
			predicate.report(3118, "Predicate is not boolean");
		}

		Type domConstraint = null;
		Type rngConstraint = null;
		
		if (constraint != null && constraint.isMap())
		{
			domConstraint = constraint.getMap().from;
			rngConstraint = constraint.getMap().to;
		}

		maptype = first.typeCheck(local, scope, domConstraint, rngConstraint);	// The map from/to type
		local.unusedCheck();
		return maptype;
	}

	@Override
	public Value eval(Context ctxt)
	{
		breakpoint.check(location, ctxt);
		ValueMap map = new ValueMap();

		try
		{
			QuantifierList quantifiers = new QuantifierList();

			for (MultipleBind mb: bindings)
			{
				ValueList bvals = mb.getBindValues(ctxt);

				for (Pattern p: mb.plist)
				{
					Quantifier q = new Quantifier(p, bvals);
					quantifiers.add(q);
				}
			}

			quantifiers.init(ctxt, false);

			while (quantifiers.hasNext())
			{
				Context evalContext = new Context(location, "map comprehension", ctxt);
				NameValuePairList nvpl = quantifiers.next();
				boolean matches = true;

				for (NameValuePair nvp: nvpl)
				{
					Value v = evalContext.get(nvp.name);

					if (v == null)
					{
						evalContext.put(nvp.name, nvp.value);
					}
					else
					{
						if (!v.equals(nvp.value))
						{
							matches = false;
							break;	// This quantifier set does not match
						}
					}
				}

				if (matches &&
					(predicate == null ||
					 predicate.eval(evalContext).boolValue(ctxt)))
				{
					Value dom = first.left.eval(evalContext);
					Value rng = first.right.eval(evalContext);
					first.location.hit();

					Value old = map.put(dom, rng);

					if (old != null && !old.equals(rng))
					{
						abort(4016, "Duplicate map keys have different values: " + dom, ctxt);
					}
				}
			}
		}
	    catch (ValueException e)
	    {
	    	return abort(e);
	    }

		return new MapValue(map);
	}

	@Override
	public Expression findExpression(int lineno)
	{
		Expression found = super.findExpression(lineno);
		if (found != null) return found;

		found = first.findExpression(lineno);
		if (found != null) return found;

		return predicate == null ? null : predicate.findExpression(lineno);
	}

	@Override
	public ProofObligationList getProofObligations(POContextStack ctxt)
	{
		ProofObligationList obligations = new ProofObligationList();

		obligations.add(new MapSetOfCompatibleObligation(this, ctxt));

		ctxt.push(new POForAllPredicateContext(this));
		obligations.addAll(first.getProofObligations(ctxt));
		ctxt.pop();

		boolean finiteTest = false;

		for (MultipleBind mb: bindings)
		{
			obligations.addAll(mb.getProofObligations(ctxt));

			if (mb instanceof MultipleTypeBind)
			{
				finiteTest = true;
			}
		}

		if (finiteTest)
		{
			obligations.add(new FiniteMapObligation(this, maptype, ctxt));
		}

		if (predicate != null)
		{
    		ctxt.push(new POForAllContext(this));
    		obligations.addAll(predicate.getProofObligations(ctxt));
    		ctxt.pop();
		}

		return obligations;
	}

	@Override
	public String kind()
	{
		return "map comprehension";
	}

	@Override
	public ValueList getValues(Context ctxt)
	{
		ValueList list = first.getValues(ctxt);

		for (MultipleBind mb: bindings)
		{
			list.addAll(mb.getValues(ctxt));
		}

		if (predicate != null)
		{
			list.addAll(predicate.getValues(ctxt));
		}

		return list;
	}

	@Override
	public LexNameList getOldNames()
	{
		LexNameList list = first.getOldNames();

		for (MultipleBind mb: bindings)
		{
			list.addAll(mb.getOldNames());
		}

		if (predicate != null)
		{
			list.addAll(predicate.getOldNames());
		}

		return list;
	}
}
