/*
 * Copyright (c) 2004-2021, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.hisp.dhis.cache;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.PeriodType;
import org.junit.Test;

/**
 * Tests the {@link Sizeof} and {@link GenericSizeof} implementation.
 *
 * @author Jan Bernitt
 */
public class SizeofTest
{

    public static class PrimitiveAndWrapperBean
    {
        int a;

        long b;

        Integer c;
    }

    public static class CollectionBean
    {
        List<Integer> a;

        List<List<Long>> b;

        CollectionBean()
        {
            this( null, null );
        }

        CollectionBean( List<Integer> a, List<List<Long>> b )
        {
            this.a = a;
            this.b = b;
        }
    }

    public static class MapBean
    {
        Map<String, Integer> a;

        Map<String, Map<Integer, Long>> b;

        public MapBean( Map<String, Integer> a, Map<String, Map<Integer, Long>> b )
        {
            this.a = a;
            this.b = b;
        }
    }

    private final GenericSizeof sizeof = new GenericSizeof( 20L, obj -> obj );

    @Test
    public void testSizeofNull()
    {
        assertEquals( 0L, sizeof.sizeof( null ) );
    }

    @Test
    public void testSizeofBoolean()
    {
        assertEquals( 24L, sizeof.sizeof( Boolean.TRUE ) );
        assertFixedSize( boolean.class, Boolean.class );

    }

    @Test
    public void testSizeofDouble()
    {
        assertEquals( 28L, sizeof.sizeof( 3d ) );
        assertFixedSize( double.class, Double.class );
    }

    @Test
    public void testSizeofLong()
    {
        assertEquals( 28L, sizeof.sizeof( 3L ) );
        assertFixedSize( long.class, Long.class );
    }

    @Test
    public void testSizeofInt()
    {
        assertEquals( 24L, sizeof.sizeof( 42 ) );
        assertFixedSize( int.class, Integer.class );
    }

    @Test
    public void testSizeofPrimitiveArray()
    {
        assertEquals( 40L, sizeof.sizeof( new byte[20] ) );
    }

    @Test
    public void testSizeofFixedArray()
    {
        // sizeof(Integer) = 24 * 10 for each Integer object
        // + 20 for array object header
        // + 10 * 4 for the reference array itself
        assertEquals( 24L * 10 + 20L + 10 * 4L, sizeof.sizeof( new Integer[10] ) );
    }

    @Test
    public void testSizeofString()
    {
        // base costs are: 20 + 20 + 8 = 48
        assertEquals( 64L, sizeof.sizeof( "hello world!" ) );
        assertEquals( 58L, sizeof.sizeof( "hello!" ) );
        assertNotFixedSize( String.class );
        assertNotFixedSize( byte[].class );
    }

    @Test
    public void testSizeofRecord()
    {
        // 20 object header of BeanA
        // + 4 int
        // + 8 long
        // + 4 ref => + 24 Integer
        assertEquals( 60L, sizeof.sizeof( new PrimitiveAndWrapperBean() ) );
        assertFixedSize( PrimitiveAndWrapperBean.class );
    }

    @Test
    public void testSizeofEmptyList()
    {
        // just the object header
        assertEquals( 24L, sizeof.sizeof( emptyList() ) );
    }

    @Test
    public void testSizeofUnknownElementTypeList()
    {
        // dynamic list as we do not have a generic type
        // 20 object header wrapper
        // 20 object header size
        // + 3 * 24 Integer objects
        // + 3 * 8 for references and list structure
        assertEquals( 116, sizeof.sizeof( asList( 1, 2, 3 ) ) );
    }

    @Test
    public void testSizeofListFields()
    {
        // 20 object header + 4 + 4 for ref fields
        assertEquals( 28L, sizeof.sizeof( new CollectionBean() ) );
        // 20 object header + 4 + 4 ref fields
        // + 20 object header of the list
        // + 3 * 24 Integer's + 3 * 8 for list structure
        // + 20 object header empty list
        assertEquals( 164L, sizeof.sizeof( new CollectionBean( asList( 1, 2, 3 ), emptyList() ) ) );
    }

    @Test
    public void testSizeofListOfListFields()
    {
        CollectionBean listOfLists = new CollectionBean( emptyList(),
            asList( asList( 1L, 2L ), asList( 3L, 4L ), asList( 5L, 6L ) ) );
        assertEquals( 368L, sizeof.sizeof( listOfLists ) );
    }

    @Test
    public void testSizeofEmptyMapFields()
    {
        assertEquals( 28L, sizeof.sizeof( new MapBean( null, null ) ) );
    }

    @Test
    public void testSizeofSingletonMapFields()
    {
        assertEquals( 159L, sizeof.sizeof( new MapBean( singletonMap( "key", 1 ), null ) ) );
    }

    @Test
    public void testSizeofHashMapFields()
    {
        Map<String, Integer> map = new HashMap<>();
        map.put( "a", 1 );
        map.put( "b", 2 );
        assertEquals( 266L, sizeof.sizeof( new MapBean( map, null ) ) );
    }

    @Test
    public void testSizeofTreeMapFields()
    {
        Map<String, Integer> map = new TreeMap<>();
        map.put( "a", 1 );
        map.put( "b", 2 );
        assertEquals( 266L, sizeof.sizeof( new MapBean( map, null ) ) );
    }

    @Test
    public void testSizeofMapOfMapsFields()
    {
        Map<String, Map<Integer, Long>> map = new HashMap<>();
        map.put( "tree", new TreeMap<>() );
        map.put( "singleton", singletonMap( 1, 42L ) );
        map.put( "empty", emptyMap() );
        assertEquals( 462L, sizeof.sizeof( new MapBean( null, map ) ) );
    }

    @Test
    public void testSizeofPeriodType()
    {
        for ( PeriodType t : PeriodType.PERIOD_TYPES )
        {
            assertTrue( sizeof.sizeof( t ) > 0L );
        }
    }

    @Test
    public void testSizeofCyclicTypeReference()
    {
        OrganisationUnit a = new OrganisationUnit();
        a.setParent( new OrganisationUnit() );
        a.setChildren( singleton( new OrganisationUnit() ) );

        assertTrue( sizeof.sizeof( a ) > 500 );
    }

    @Test
    public void testSizeofCyclicValueReferences()
    {
        OrganisationUnit a = new OrganisationUnit();
        OrganisationUnit parent = new OrganisationUnit();
        a.setParent( parent );
        parent.setChildren( singleton( a ) );

        long blankSize = sizeof.sizeof( a );
        assertTrue( blankSize > 500 );
        assertEquals( blankSize, sizeof.sizeof( a ) );

        // now setting a field should come out identical to adding the size of
        // the value of the field
        a.setName( "UnitA" );
        assertEquals( sizeof.sizeof( "UnitA" ), sizeof.sizeof( a ) - blankSize );
    }

    private void assertFixedSize( Class<?>... types )
    {
        for ( Class<?> t : types )
        {
            assertFixedSize( t );
        }
    }

    private void assertFixedSize( Class<?> type )
    {
        assertTrue( type.getSimpleName() + " not detected as fixed size", sizeof.isFixedSize( type ) );
    }

    private void assertNotFixedSize( Class<?> type )
    {
        assertFalse( type.getSimpleName() + " wrongly considered as fixed size", sizeof.isFixedSize( type ) );
    }
}