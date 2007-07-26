/*
 * Bytecode Analysis Framework
 * Copyright (C) 2004, University of Maryland
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package edu.umd.cs.findbugs.ba;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.InvokeInstruction;

/**
 * A simple class to parse method signatures.
 *
 * @author David Hovemeyer
 */
public class SignatureParser {
	int totalArgumentSize;
	int parameterOffset[] = null;

	private void calculateOffsets() {
		if (parameterOffset != null) return;
		ArrayList<Integer> offsets = new ArrayList<Integer>();
		Iterator<String> i = parameterSignatureIterator();
		totalArgumentSize = 0;
		while(i.hasNext()) {
			String s = i.next();

			if (s.equals("D") || s.equals("J")) totalArgumentSize += 2;
			else totalArgumentSize += 1;
			offsets.add(totalArgumentSize);
		}
		parameterOffset = new int[offsets.size()];
		for(int j = 0; j < offsets.size(); j++)
			parameterOffset[j] = offsets.get(j);
	}

	public int getSlotsFromTopOfStackForParameter(int paramNum) {
		calculateOffsets();
		int result = totalArgumentSize - parameterOffset[paramNum];
		return result;
	}

	private class ParameterSignatureIterator implements Iterator<String> {
		private int index = 1;

		public boolean hasNext() {
			return index < signature.length()
					&& signature.charAt(index) != ')';
		}

		public String next() {
			if (!hasNext()) throw new NoSuchElementException();
			StringBuffer result = new StringBuffer();
			boolean done;
			do {
				done = true;
				int ch = signature.charAt(index);
				switch (ch) {
				case 'B':
				case 'C':
				case 'D':
				case 'F':
				case 'I':
				case 'J':
				case 'S':
				case 'Z':
					result.append(signature.charAt(index));
					++index;
					break;

				case 'L':
					int semi = signature.indexOf(';', index + 1);
					if (semi < 0)
						throw new IllegalStateException("Invalid method signature: " + signature);
					result.append(signature.substring(index, semi + 1));
					index = semi + 1;
					break;

				case '[':
					result.append('[');
					++index;
					done = false;
					break;

				case 'V':
				default:
					throw new IllegalStateException("Invalid method signature: " + signature);
				}
			} while (!done);

			return result.toString();
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	private final String signature;

	@Override
	public String toString() {
		return signature;
	}
	/**
	 * Constructor.
	 *
	 * @param signature the method signature to be parsed
	 */
	public SignatureParser(String signature) {
		if (!signature.startsWith("("))
			throw new IllegalArgumentException("Bad method signature: " + signature);
		this.signature = signature;
	}

	/**
	 * Get an Iterator over signatures of the method parameters.
	 *
	 * @return Iterator which returns the parameter type signatures in order
	 */
	public Iterator<String> parameterSignatureIterator() {
		return new ParameterSignatureIterator();
	}

	/**
	 * Get the method return type signature.
	 * 
	 * @return the method return type signature
	 */
	public String getReturnTypeSignature() {
		int endOfParams = signature.lastIndexOf(')');
		if (endOfParams < 0)
			throw new IllegalArgumentException("Bad method signature: " + signature);
		return signature.substring(endOfParams + 1);
	}

	/**
	 * Get the number of parameters in the signature.
	 * 
	 * @return the number of parameters
	 */
	public int getNumParameters() {
		int count = 0;
		for (Iterator<String> i = parameterSignatureIterator(); i.hasNext();) {
			i.next();
			++count;
		}
		return count;
	}

	public String getParameter(int pos) {
		int count = 0;
		for (Iterator<String> i = parameterSignatureIterator(); i.hasNext();) {
			String p = i.next();
			if (pos == count) return p;
			++count;
		}
		throw new IndexOutOfBoundsException("Asked for parameter " + pos + " of " + signature);
	}


	/**
	 * Determine whether or not given signature denotes a reference type.
	 * 
	 * @param signature a signature
	 * @return true if signature denotes a reference type, false otherwise
	 */
	public static boolean isReferenceType(String signature) {
		return signature.startsWith("L") || signature.startsWith("[");
	}
	
	/**
	 * Get the number of parameters passed to method invocation.
	 * 
	 * @param inv
	 * @param cpg
	 * @return int number of parameters
	 */
	public static int getNumParametersForInvocation(InvokeInstruction inv, ConstantPoolGen cpg) {
		SignatureParser sigParser = new SignatureParser(inv.getSignature(cpg));
		return sigParser.getNumParameters();
	}
	
	/**
	 * Return how many stack frame slots a type whose signature
	 * is given will occupy.  long and double values take 2 slots,
	 * while all other kinds of values take 1 slot. 
	 * 
	 * @param sig a type signature
	 * @return number of stack frame slots a value of the given type will occupy
	 */
	public static int getNumSlotsForType(String sig) {
		if (sig.equals("J") || sig.equals("D")) {
			return 2;
		} else {
			return 1;
		}
	}

	public static void main(String[] args) {
		if (args.length != 1) {
			System.err.println("Usage: " + SignatureParser.class.getName() + " '<method signature>'");
			System.exit(1);
		}
		SignatureParser parser = new SignatureParser(args[0]);
		for (Iterator<String> i = parser.parameterSignatureIterator(); i.hasNext();){
			System.out.println(i.next());
		}
		System.out.println(parser.getNumParameters() + " parameter(s)");
	}
}

// vim:ts=4
