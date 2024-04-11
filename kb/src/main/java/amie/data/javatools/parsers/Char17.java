package amie.data.javatools.parsers;

/**
 * This class is part of the Java Tools (see
 * http://mpii.de/yago-naga/javatools). It is licensed under the Creative
 * Commons Attribution License (see http://creativecommons.org/licenses/by/3.0)
 * by the YAGO-NAGA team (see http://mpii.de/yago-naga).
 * <p>
 * This class is the improved version of the original Char class, which takes
 * into account that Java 1.7 performs a copy for substring()
 * <p>
 * This class provides static methods to <I>decode, encode</I> and
 * <I>normalize</I> Strings.<BR>
 * <B>Decoding</B> converts the following codes to Java 16-bit characters (
 * <TT>char</TT>):
 * <UL>
 * <LI>all HTML ampersand codes (like &amp;nbsp;) as specified by the W3C
 * <LI>all backslash codes (like \ b) as specified by the Java language
 * specification
 * <LI>all percentage codes (like %2C) as used in URLs and E-Mails
 * <LI>all UTF-8 codes (like ī) as specified in Wikipedia
 * </UL>
 * <P>
 * <B>Encoding</B> is the inverse operation. It takes a Java 16-bit character (
 * <TT>char</TT>) and outputs its encoding in HTML, as a backslash code, as a
 * percentage code or in UTF8.
 * <P>
 * <B>Normalization</B> converts the following Unicode characters (Java 16-bit
 * <TT>char</TT>s) to ASCII-characters in the range 0x20-0x7F:
 * <UL>
 * <LI>all ASCII control characters (0x00-0x1F)
 * <LI>all Latin-1 characters (0x80-0xFF) to the closest transliteration
 * <LI>all Latin Extended-A characters (0x100-0x17F) to the closest
 * transliteration
 * <LI>all Greek characters (0x374-0x3D6) to the closest transliteration as
 * specified in Wikipedia
 * <LI>all General-Punctuation characters (0x2016-0x2055) to the closest ASCII
 * punctuation
 * <LI>most mathematical symbols (in the range of 0x2000) to the common program
 * code identifier or text
 * <LI>all ligatures (0xFB00-0xFB06, the nasty things you get when you
 * copy/paste from PDFs) to the separate characters
 * </UL>
 */
//public class Char17 {
//
//
//	public static String cutLast(String s) {
//		return (s.length() == 0 ? "" : s.substring(0, s.length() - 1));
//	}
//
//	/** Test routine */
//	public static void main(String argv[]) throws Exception {
//		//test();
//	}
//
//}
