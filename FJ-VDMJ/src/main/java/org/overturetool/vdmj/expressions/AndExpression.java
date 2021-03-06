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

import org.overturetool.vdmj.definitions.DefinitionList;
import org.overturetool.vdmj.lex.LexToken;
import org.overturetool.vdmj.pog.POContextStack;
import org.overturetool.vdmj.pog.POImpliesContext;
import org.overturetool.vdmj.pog.ProofObligationList;
import org.overturetool.vdmj.pog.SubTypeObligation;
import org.overturetool.vdmj.runtime.Context;
import org.overturetool.vdmj.runtime.ValueException;
import org.overturetool.vdmj.typechecker.Environment;
import org.overturetool.vdmj.typechecker.FlatEnvironment;
import org.overturetool.vdmj.typechecker.NameScope;
import org.overturetool.vdmj.types.BooleanType;
import org.overturetool.vdmj.types.Type;
import org.overturetool.vdmj.types.TypeList;
import org.overturetool.vdmj.values.BooleanValue;
import org.overturetool.vdmj.values.Value;

public class AndExpression extends BooleanBinaryExpression
{
	private static final long serialVersionUID = 1L;

	public AndExpression(Expression left, LexToken op, Expression right)
	{
		super(left, op, right);
	}

	@Override
	public DefinitionList getQualifiedDefs(Environment env)
	{
		DefinitionList result = left.getQualifiedDefs(env);
		result.addAll(right.getQualifiedDefs(env));
		return result;
	}
	
	@Override
	public Type typeCheck(Environment env, TypeList qualifiers, NameScope scope, Type constraint)
	{
		DefinitionList qualified = left.getQualifiedDefs(env);
		Environment qenv = env;
		
		if (!qualified.isEmpty())
		{
			qenv = new FlatEnvironment(qualified, env);
		}

		return super.typeCheck(qenv, qualifiers, scope, constraint);
	}

	@Override
	public Value eval(Context ctxt)
	{
		// breakpoint.check(location, ctxt);
		location.hit();		// Mark as covered

		try
		{
			Value lv = left.eval(ctxt);

			if (lv.isUndefined())
			{
				return lv;
			}

			boolean lb = lv.boolValue(ctxt);

			if (!lb)
			{
				return lv;	// Stop after LHS
			}

			Value rv = right.eval(ctxt);

			if (lb)
			{
				return rv;
			}

			return new BooleanValue(false);
		}
		catch (ValueException e)
		{
			return abort(e);
		}
	}

	@Override
	public ProofObligationList getProofObligations(POContextStack ctxt)
	{
		ProofObligationList obligations = new ProofObligationList();

		if (ltype.isUnion())
		{
			obligations.add(
				new SubTypeObligation(left, new BooleanType(left.location), ltype, ctxt));
		}

		if (rtype.isUnion())
		{
			ctxt.push(new POImpliesContext(left));
			obligations.add(new SubTypeObligation(
				right, new BooleanType(right.location), rtype, ctxt));
			ctxt.pop();
		}

		obligations.addAll(left.getProofObligations(ctxt));

		ctxt.push(new POImpliesContext(left));
		obligations.addAll(right.getProofObligations(ctxt));
		ctxt.pop();

		return obligations;
	}

	@Override
	public String kind()
	{
		return "and";
	}
}
