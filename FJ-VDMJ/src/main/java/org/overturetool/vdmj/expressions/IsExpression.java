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

import org.overturetool.vdmj.definitions.Definition;
import org.overturetool.vdmj.definitions.DefinitionList;
import org.overturetool.vdmj.definitions.QualifiedDefinition;
import org.overturetool.vdmj.lex.LexLocation;
import org.overturetool.vdmj.lex.LexNameList;
import org.overturetool.vdmj.lex.LexNameToken;
import org.overturetool.vdmj.pog.POContextStack;
import org.overturetool.vdmj.pog.ProofObligationList;
import org.overturetool.vdmj.runtime.Context;
import org.overturetool.vdmj.runtime.ContextException;
import org.overturetool.vdmj.runtime.ValueException;
import org.overturetool.vdmj.typechecker.Environment;
import org.overturetool.vdmj.typechecker.NameScope;
import org.overturetool.vdmj.typechecker.TypeComparator;
import org.overturetool.vdmj.types.BooleanType;
import org.overturetool.vdmj.types.Type;
import org.overturetool.vdmj.types.TypeList;
import org.overturetool.vdmj.values.BooleanValue;
import org.overturetool.vdmj.values.RecordValue;
import org.overturetool.vdmj.values.Value;
import org.overturetool.vdmj.values.ValueList;

public class IsExpression extends Expression
{
	private static final long serialVersionUID = 1L;
	public Type basictype;
	public final LexNameToken typename;
	public final Expression test;

	private Definition typedef = null;

	public IsExpression(LexLocation location, LexNameToken typename, Expression test)
	{
		super(location);
		this.basictype = null;
		this.typename = typename;
		this.test = test;
	}

	public IsExpression(LexLocation location, Type type, Expression test)
	{
		super(location);
		this.basictype = type;
		this.typename = null;
		this.test = test;
	}

	@Override
	public String toString()
	{
		return "is_(" + test + ", " + (typename == null ? basictype : typename) + ")";
	}

	@Override
	public Type typeCheck(Environment env, TypeList qualifiers, NameScope scope, Type constraint)
	{
		test.typeCheck(env, null, scope, null);

		if (basictype != null)
		{
			basictype = basictype.typeResolve(env, null);
			TypeComparator.checkComposeTypes(basictype, env, false);
		}

		if (typename != null)
		{
			typedef = env.findType(typename, location.module);

			if (typedef == null)
			{
				report(3113, "Unknown type name '" + typename + "'");
			}
		}

		return checkConstraint(constraint, new BooleanType(location));
	}

	@Override
	public DefinitionList getQualifiedDefs(Environment env)
	{
		DefinitionList result = new DefinitionList();
		
		if (test instanceof VariableExpression)
		{
			VariableExpression exp = (VariableExpression)test;
			Definition existing = env.findName(exp.name, NameScope.NAMESANDSTATE);
			
			if (existing != null && existing.nameScope.matches(NameScope.NAMES))
			{
        		if (basictype != null)
        		{
       				result.add(new QualifiedDefinition(existing, basictype));
        		}
        		else if (typename != null)
        		{
        			if (typedef == null)
        			{
        				typedef = env.findType(typename, location.module);
        			}

        			if (typedef != null)
        			{
        				result.add(new QualifiedDefinition(existing, typedef.getType()));
        			}
        		}
			}
		}
		
		return result;
	}
	
	@Override
	public Value eval(Context ctxt)
	{
		breakpoint.check(location, ctxt);

		Value v = test.eval(ctxt);

		try
		{
    		if (typename != null)
    		{
    			if (typedef != null)
    			{
    				if (typedef.isTypeDefinition())
    				{
    					// NB. we skip the DTC enabled check here
    					v.convertValueTo(typedef.getType(), ctxt);
    					return new BooleanValue(true);
    				}
    			}
    			else if (v.isType(RecordValue.class))
    			{
    				RecordValue rv = v.recordValue(ctxt);
    				return new BooleanValue(rv.type.name.equals(typename));
    			}
    		}
    		else
    		{
    			// NB. we skip the DTC enabled check here
   				v.convertValueTo(basictype, ctxt);
   				return new BooleanValue(true);
    		}
		}
		catch (ContextException ex)
		{
			if (ex.number != 4060)	// Type invariant violation
			{
				throw ex;	// Otherwise return false
			}
		}
		catch (ValueException ex)
		{
			// return false...
		}

		return new BooleanValue(false);
	}

	@Override
	public Expression findExpression(int lineno)
	{
		Expression found = super.findExpression(lineno);
		if (found != null) return found;

		return test.findExpression(lineno);
	}

	@Override
	public ProofObligationList getProofObligations(POContextStack ctxt)
	{
		if (typedef != null)
		{
			ctxt.noteType(test, typedef.getType());
		}
		else if (basictype != null)
		{
			ctxt.noteType(test, basictype);
		}

		return test.getProofObligations(ctxt);
	}

	@Override
	public String kind()
	{
		return "is_";
	}

	@Override
	public ValueList getValues(Context ctxt)
	{
		return test.getValues(ctxt);
	}

	@Override
	public LexNameList getOldNames()
	{
		return test.getOldNames();
	}
}
