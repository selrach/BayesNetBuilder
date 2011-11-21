package selrach.bnetbuilder.gui.notification.io;

import java.io.OutputStream;
import java.io.PrintStream;

import javax.swing.JTextArea;

/**
 * This handles the attachment of a print stream to a text component window.
 * 
 * @author <a href="mailto:charleswrobertson@gmail.com">Charles Robertson</a>
 * 
 */
public class TextComponentPrintStream extends PrintStream {

	final private JTextArea area;

	public TextComponentPrintStream(JTextArea passedArea) {
		super(new OutputStream() {
			@Override
			public void write(int i) {
			}
		});
		this.area = passedArea;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.PrintStream#print(java.lang.String)
	 */
	@Override
	public void print(String s) {
		area.append(s);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.PrintStream#println(java.lang.String)
	 */
	@Override
	public void println(String s) {
		print(s + System.getProperty("line.separator"));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.PrintStream#print(double)
	 */
	@Override
	public void print(double d) {
		print("" + d);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.PrintStream#print(boolean)
	 */
	@Override
	public void print(boolean b) {
		print("" + b);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.PrintStream#print(char)
	 */
	@Override
	public void print(char c) {
		print("" + c);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.PrintStream#print(char[])
	 */
	@Override
	public void print(char[] s) {
		print(s.toString());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.PrintStream#print(float)
	 */
	@Override
	public void print(float f) {
		print("" + f);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.PrintStream#print(int)
	 */
	@Override
	public void print(int i) {
		print("" + i);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.PrintStream#print(long)
	 */
	@Override
	public void print(long l) {
		print("" + l);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.PrintStream#print(java.lang.Object)
	 */
	@Override
	public void print(Object obj) {
		print(obj.toString());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.PrintStream#println()
	 */
	@Override
	public void println() {
		println("");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.PrintStream#println(boolean)
	 */
	@Override
	public void println(boolean x) {
		println("" + x);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.PrintStream#println(char)
	 */
	@Override
	public void println(char x) {
		println("" + x);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.PrintStream#println(char[])
	 */
	@Override
	public void println(char[] x) {
		println("" + x.toString());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.PrintStream#println(double)
	 */
	@Override
	public void println(double x) {
		println("" + x);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.PrintStream#println(float)
	 */
	@Override
	public void println(float x) {
		println("" + x);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.PrintStream#println(int)
	 */
	@Override
	public void println(int x) {
		println("" + x);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.PrintStream#println(long)
	 */
	@Override
	public void println(long x) {
		println("" + x);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.PrintStream#println(java.lang.Object)
	 */
	@Override
	public void println(Object x) {
		println("" + x);
	}
}
