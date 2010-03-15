package org.parandroid.compression;

//////////////////////////////////////////////////////////////////////////////////////
// File: Huffman.java                Date: 12/11/1999		      Version: 1.00 //
// -------------------------------------------------------------------------------- //
//  										    //
// Dynamic huffman coder class for java.				    	    //
// Written from scratch and copyright (c) 1999 by Gerald Friedland.		    //
//										    //
// This program is released under the GNU Library Public License 2.00. 	            //  
// You must agree to this license before using, copying or modifying this	    //
// piece of code.								    //
//										    //
// You should have received a copy of the GNU Library General Public		    //
// License along with this class (see the file COPYING.LIB); if not, write to the   // 
// Free Software Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.          //
//										    //
// Please send bug reports, improvements, or proposals to: fractor@germanymail.com  //
// Please consult the file README for details of using this software.  		    //
// -------------------------------------------------------------------------------- //
//  										    //
// 06/28/1999 Version 0.99e -- unofficial pre-version				    //
// 12/11/1999 Version 1.00 -- release, but still needs to be optimized heavily.     //
//////////////////////////////////////////////////////////////////////////////////////

import java.lang.*;
import java.io.*;
import java.math.*;

// ----------------------------- will be needed... -------------------
class node {
        public int edge1;
        public int edge2;
}

class chartableentry {
        public long count;
        public int character;
}

public class Huffman {

// ----------------------------- constants ---------------------------

public static final String VERSION = "1.00";
public static final int ALPHABETSIZE = 256;

// ----------------------------- static variables --------------------
private short usedchars;
private String[] transtable = new String[ALPHABETSIZE];  
private node[] tree=new node[ALPHABETSIZE*2];

// ----------------------------- general code ------------------------
static {
}

// Convert signed byte to unsigned byte (in Java we must use short for this)
public static short makeunsigned(byte x)
{
        return ( (x<0) ? (short) (x+256) : (short) x);
}    


// ----------------------------- compress ----------------------------

// Generate a lookuptable with the frequencies of the char in the alphabet
void gencounttable (byte[] datain, long[] counttable) 
{
	int i;
        for (i=0;i<counttable.length;i++) counttable[i]=0;
	for (i=0;i<datain.length;i++) {
		counttable[makeunsigned(datain[i])]=counttable[makeunsigned(datain[i])]+1;
	}
}

// Get the lowest frequented char
int getlowest(chartableentry[] chartable)
{
	int min=0;
	while (chartable[min].count==0) {
		min++;
		if (min==chartable.length) return(-1);
	}
	for (int i=min;i<chartable.length;i++) {
		if ((chartable[min].count>chartable[i].count) && (chartable[i].count>0)) min=i;
	}
	return(min);
}

// Builds a translation table from a given huffman tree.
void buildtranstable(node[] tree, int i, String code)
{
//	System.out.println("bt(tree,"+i+","+code+")");
	if (tree[i].edge1>=0) {
		transtable[tree[i].edge1]=code+"0";
	} else buildtranstable(tree,Math.abs(tree[i].edge1),code+0);
	if (tree[i].edge2>=0) {
		transtable[tree[i].edge2]=code+"1";
	} else buildtranstable(tree,Math.abs(tree[i].edge2),code+1);
}


// Generates huffman tree and then calls buildtranstable().
int generatecode (byte[] data, int[] treeout)
{
	int i,index,l;
	long k,n=0,addi;
	int j;
	long [] counttable = new long[ALPHABETSIZE]; 
	for (i=0;i<ALPHABETSIZE;i++) transtable[i] = new String();
	chartableentry[] chartable = new chartableentry[ALPHABETSIZE];
	node[] tree=new node[ALPHABETSIZE*2];
	gencounttable(data,counttable);
	usedchars=0;
	for (i=0;i<ALPHABETSIZE;i++) {
		chartable[i] = new chartableentry();
		if (counttable[i]!=0) {
		        chartable[usedchars].count=counttable[i];
			chartable[usedchars].character=(short) i;
		        usedchars++;
		}	
	}
	index=getlowest(chartable);
	tree[0]=new node(); // We do not use tree[0] but we must
	tree[0].edge1=0;    // initialize for not getting a
	tree[0].edge2=0;    // Null Pointer exception... :(
	j=1;
 	while (index!=-1) {
		tree[j]=new node();
		tree[j].edge1=chartable[index].character;
		addi=chartable[index].count;
		chartable[index].count=0;
		index=getlowest(chartable);
		if (index!=-1) {
			tree[j].edge2=chartable[index].character;
			chartable[index].count+=addi;
			chartable[index].character=(-1)*j;
		} else {
			tree[j].edge2=(-1)*j;
		}
		index=getlowest(chartable);
		j++;
	
	}
	buildtranstable(tree,j-2,""); 
	i=0;
        l=1;
        while (l<j-1) {
                treeout[i]=tree[l].edge1;
                treeout[i+1]=tree[l].edge2;
                i+=2;
                l+=1;
        }
	return(j-2);
}

// This routine sets bit n in b.
byte BitSet(byte b, int n)
{
	b|=1<<(n-1);
	return(b);
}

// Convert a dual number representation in String format to an array of bytes.
void String2Bytes(String s, byte[] barray)
{
	int i,j,l=s.length()/8;
	for (j=0;j<l;j++) {
		for (i=0;i<8;i++) {	
			if (s.charAt(i+(j*8))=='1') barray[j]=BitSet(barray[j],8-i);
		}
	}
	l=s.length()%8;
	for (i=0;i<l;i++) {
        	if (s.charAt(i+(j*8))=='1') barray[j]=BitSet(barray[j],8-i);
	}
}


// ----------------------------- uncompress --------------------------
// Does what name says...
void uncode(String s, byte[] dataout, int[] tree)
{
	int outcount=0;
	int incount=0;
	int treesize=0;
	int treecount=0;
	while (!((tree[treesize]==0) && (tree[treesize+1]==0))) treesize++;
	treecount=treesize-2;
	while ((outcount<dataout.length) && (incount<s.length())) {
		if (s.charAt(incount)=='0') {
			if (tree[treecount]>=0) {
				dataout[outcount]=(byte) tree[treecount];
				treecount=treesize-2;
				outcount++;
			} else {
				treecount=(Math.abs(tree[treecount]*2)-2);
			} 
		}
		if (s.charAt(incount)=='1') {
			if (tree[treecount+1]>=0) {
				dataout[outcount]=(byte) tree[treecount+1];
				treecount=treesize-2;
				outcount++;
			} else {
				treecount=(Math.abs(tree[treecount+1])*2)-2;
			} 			
		}
		incount++;
	}
}

// Convert decimal number to dual number fitted to 8^n bits.
String dual(int i)
{
	int h=1;
	String s="";
	while (h<=i/2) h*=2;
	while (h>0) {
		s+=(i/h);
		i=i%h;
		h=h/2;
	}
	if (s.length()%8!=0) {
		h=s.length()/8;
		h=((h+1)*8)-s.length();
		for (int j=0;j<h;j++) s="0"+s;
	}
	return(s);
}


// ----------------------------- interface ---------------------------
// compress
// compresses datain and gives the result in dataout 
// and the decoding tree in treeout.
// Returns the length of the resulting code as integer value.
public int compress(byte[] datain, byte[] dataout, int[] treeout)
{
	int len,j,l,i;
	String s = "";
	j=generatecode(datain,treeout);
	for (i=0;i<datain.length;i++) s+=transtable[makeunsigned(datain[i])];
	String2Bytes(s,dataout);
	len=s.length()/8;
	if ((s.length()%8)>0) len++; 
	return(len);			
}

public int treesize(int[] treein)
{
	int ts=0;
	while (!((treein[ts]==0) && (treein[ts+1]==0))) ts++;
	return ts;
}


// uncompresses datain using tree into dataout.
// dataout will be filled up exactly (-> may not be longer than code expected!)
public void uncompress(byte[] datain, byte dataout[], int[] treein)
{
	int i;
	String s="";
	for (i=0;i<datain.length;i++) s+=dual(makeunsigned(datain[i]));		
	uncode(s,dataout,treein);
}

// Test and demonstration method.
public static void main(String[] args) 
{
	Huffman h = new Huffman();
	System.out.println();
	System.out.println("Dynamic Huffman Compression Class for Java.");
	System.out.println("Version "+VERSION);
	System.out.println("Copyright (c) 1999 by Gerald Friedland");
	System.out.println("This library is free software according to the terms");
	System.out.println("of the GNU Library Public License 2.00 (see COPYING.LIB).\n\n");
	if ((args.length!=1) && (args.length!=2)) { 
		System.out.println("huffman <teststring> [d]");
		System.out.println("If you specify a second argument, you will get debugging output"); 		
		return;
	}
	int i,l2,ts;
	int l=args[0].length();
	int huftree[] = new int[ALPHABETSIZE];
	byte dataout2[] = new byte[l];
	byte datain[] = new byte[l];
	byte dataout[] = new byte[l];
	
	for (i=0;i<l;i++) datain[i]=(byte) args[0].charAt(i); 
	
	if (args.length==2) {
		System.out.println("Input (ASCII Code):");
		for (i=0;i<l;i++) System.out.print(makeunsigned(datain[i])+" ");
	}
	
	l=datain.length;
	l2=h.compress(datain,dataout,huftree);
	ts=h.treesize(huftree);
	if (args.length==2) {
		System.out.println("\nCompressed (New Code):");
		for (i=0;i<l2;i++) System.out.print((byte) dataout[i]+" ");
	}
	
	h.uncompress(dataout,dataout2,huftree);
	
	if (args.length==2) {	
		System.out.println("\nUncompressed:");
		for (i=0;i<dataout.length;i++) System.out.print((byte) dataout2[i]+" ");
		System.out.println();
		System.out.println();
		System.out.println("Tree length: "+ts);
	}
	
	System.out.println("Ratio (without tree):   1:"+(float) l/l2); 
	System.out.print  ("Real ratio (with tree): 1:"+(float) l/(l2+ts));	
	if (l/(l2+ts)<1) System.out.println(" => INFLATION!"); else System.out.println();
}
}