/*******************************************************************************
 * Copyright 2010 Cees De Groot, Alex Boisvert, Jan Kotek
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package jdbm;

import java.io.IOError;
import java.io.IOException;
import java.util.*;

/**
 * An abstract class implementing most of RecordManager.
 * It also has some JDBM package protected stuff (getNamedRecord)
 */
 abstract class RecordManager2 implements RecordManager {



    public long insert(Object obj) throws IOException{
        return insert(obj,defaultSerializer());
    }


    public void update(long recid, Object obj) throws IOException{
        update(recid,obj, defaultSerializer());
    }


    public <A> A fetch(long recid) throws IOException{
        return (A) fetch(recid,defaultSerializer());
    }

    public <K, V> PrimaryHashMap<K, V> loadHashMap(String name) {
        try{
            long recid = assertNameExist(name);
            HTree tree = fetch(recid);
            tree.setPersistenceContext(this, recid);
            return tree;
        }catch(IOException  e){
            throw new IOError(e);
        }
    }

    public <K, V> PrimaryHashMap<K, V> createHashMap(String name) {
        return createHashMap(name, null, null);
    }


    public synchronized <K, V> PrimaryHashMap<K, V> createHashMap(String name, Serializer<K> keySerializer, Serializer<V> valueSerializer) {
		try{
                    assertNameNotExist(name);

                    long recid = insert(null);
                    HTree<K, V> tree = new HTree(this, recid, keySerializer, valueSerializer);
                    update(recid,tree);
                    setNamedObject( name, recid );

		    return tree;
		}catch(IOException  e){
		    throw new IOError(e);
		}
	}

        public synchronized <K> Set<K> loadHashSet(String name) {
        return new HTreeSet(loadHashMap(name));
        }

        public synchronized <K> Set<K> createHashSet(String name) {
            return createHashSet(name,null);
        }

        public synchronized <K> Set<K> createHashSet(String name, Serializer<K> keySerializer) {
                return new HTreeSet(createHashMap(name,keySerializer,null));
	}

    public <K, V> PrimaryTreeMap<K, V> loadTreeMap(String name) {
        try{
            long recid = assertNameExist(name);
            return BTree.<K,V>load( this, recid ).asMap();
        }catch(IOException  e){
	    throw new IOError(e);
	}
    }

    public <K extends Comparable, V> PrimaryTreeMap<K, V> createTreeMap(String name) {
		return createTreeMap(name, null,null,null);
	}


    public synchronized <K, V> PrimaryTreeMap<K, V> createTreeMap(String name,
                                                                  Comparator<K> keyComparator,
                                                                  Serializer<K> keySerializer,
                                                                  Serializer<V> valueSerializer) {
		try{
	                assertNameNotExist(name);

			BTree<K,V> tree = BTree.createInstance(this,keyComparator);

			tree.setKeySerializer(keySerializer);
			tree.setValueSerializer(valueSerializer);
                        setNamedObject( name, tree.getRecid() );
			
			return tree.asMap();
		}catch(IOException  e){
			throw new IOError(e);
		}	
	}


        public synchronized <K> SortedSet<K> loadTreeSet(String name) {
            return new BTreeSet<K>((SortedMap <K, Object>)loadTreeMap(name));
        }

        public synchronized <K> SortedSet<K> createTreeSet(String name) {
            return createTreeSet(name, null, null);
        }


        public synchronized <K> SortedSet<K> createTreeSet(String name, Comparator<K> keyComparator, Serializer<K> keySerializer) {
            return new BTreeSet<K>(createTreeMap(name,keyComparator, keySerializer, null));
        }


    public <K> List<K> createLinkedList(String name){
        return createLinkedList(name, null);
    }

    public <K> List<K> createLinkedList(String name, Serializer<K> serializer){
        try{
            assertNameNotExist(name);

             //allocate record and overwrite it
            long recid = insert(null);
            LinkedList<K> list = new LinkedList<K>(this,recid,serializer);
            update(recid,list);
            setNamedObject( name, recid );


            return list;
        }catch (IOException e){
            throw new IOError(e);
        }
    }

    public <K> List<K> loadLinkedList(String name){
        try{
            long recid = assertNameExist(name);

            LinkedList<K> list = (LinkedList<K>) fetch(recid);
            list.setRecmanAndListRedic(this, recid);
            return list;
        }catch (IOException e){
            throw new IOError(e);
        }
    }


    private void assertNameNotExist(String name) throws IOException {
        if(getNamedObject(name)!=0)
            throw new IllegalArgumentException("Object with name '"+name+"' already exists");
    }

    private long assertNameExist(String name) throws IOException {
        long recid = getNamedObject( name);
        if ( recid == 0 )
            throw new IllegalArgumentException("Object with name '"+name+"' does not exist");
        return recid;
    }




    /**
     * Obtain the record id of a named object. Returns 0 if named object
     * doesn't exist.
     * Named objects are used to store Map views and other well known objects.
     */
    protected abstract long getNamedObject( String name )
        throws IOException;


    /**
     * Set the record id of a named object.
     * Named objects are used to store Map views and other well known objects.
     */
    protected abstract void setNamedObject( String name, long recid )
        throws IOException;

    protected abstract Serializer defaultSerializer();
	
}
