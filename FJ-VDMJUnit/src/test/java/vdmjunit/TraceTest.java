/*******************************************************************************
 *
 *	Copyright (c) 2013 Fujitsu Services Ltd.
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

package vdmjunit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.overturetool.vdmj.Release;
import org.overturetool.vdmj.traces.TraceReductionType;

import vdmjunit.VDMJUnitTestSL;

public class TraceTest extends VDMJUnitTestSL
{
	@BeforeClass
	public static void start() throws Exception
	{
		setRelease(Release.VDM_10);
		readSpecification("traces.vdm");
	}
	
	@Before
	public void setUp()
	{
		init();
	}
	
	@Test
	public void one() throws Exception
	{
		assertFalse(runTrace("T1"));
	}
	
	@Test
	public void two() throws Exception
	{
		assertTrue(runTrace("T1", 3));
	}
	
	@Test
	public void three() throws Exception
	{
		assertFalse(runTrace("T1", 0.5, TraceReductionType.RANDOM, 123));
	}
}
