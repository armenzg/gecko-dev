/* -*- Mode: C++; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * The contents of this file are subject to the Netscape Public License
 * Version 1.0 (the "NPL"); you may not use this file except in
 * compliance with the NPL.  You may obtain a copy of the NPL at
 * http://www.mozilla.org/NPL/
 *
 * Software distributed under the NPL is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the NPL
 * for the specific language governing rights and limitations under the
 * NPL.
 *
 * The Initial Developer of this code under the NPL is Netscape
 * Communications Corporation.  Portions created by Netscape are
 * Copyright (C) 1998 Netscape Communications Corporation.  All Rights
 * Reserved.
 */
/*
 * @(#) LDAPModify.java
 *
 */

import java.io.*;
import java.net.*;
import java.util.*;
import netscape.ldap.*;
import netscape.ldap.util.*;

/**
 * Executes modify, delete, add, and modRDN.
 * This class is implemented based on the java LDAP classes.
 *
 * <pre>
 * usage       : java LDAPModify [options]
 * example     : java LDAPModify -w "password" -h ldap.netscape.com -p 389
 *                -f modify.cfg
 *
 * options: {np = no parameters, p = requires parameters}
 *  'D' bind DN --------------------------------------------- p
 *  'f' input file ------------------------------------------ p
 *  'h' LDAP host ------------------------------------------- p
 *  'p' LDAP port ------------------------------------------- p
 *  'n' override DN to modify ------------------------------- p
 *  'w' bind password --------------------------------------- p
 *  'e' record rejected records in a text file -------------- p
 *  'c' continuous, do not stop on error
 *  'a' add, if no operation is specified
 *  'r' replace, if no operation is specified
 *  'b' binary, read values starting with / from a file
 *
 * note: '-' or '/' is used to distinct the option field.
 *       e.g. -a -b /c /d parameter -e parameter
 *
 * </pre>
 *
 * @version 1.0
 */

public class LDAPModify extends LDAPTool { /* LDAPModify */

    public static void main(String args[]) { /* main */

		/* extract parameters from the arguments list */
		extractParameters(args);

		/* perform an LDAP client connection operation */
		try {
			if (!m_justShow) {
				m_client = new LDAPConnection();
				m_client.connect( m_ldaphost, m_ldapport );
			}
		} catch(Exception e) {
			System.err.println("Error: client connection failed!");
			System.exit(0);
		}

		/* perform an LDAP bind operation */
		try {
			if (!m_justShow) 
				m_client.authenticate( m_version, m_binddn, m_passwd );
		} catch (Exception e) {
			System.err.println( e.toString() );
			System.exit(0);
		}

		try {
			if ( m_file != null )
				m_ldif = new LDIF(m_file);
			else {
				m_ldif = new LDIF();
}
		} catch (Exception e) {
			if ( m_file == null )
				m_file = "stdin";
			System.err.println("Failed to read LDIF file " + m_file +
							   ", " + e.toString());
			System.exit(0);
		}

		/* performs a JDAP Modify operation */
		try {
			doModify();
		} catch (Exception e) {
			System.err.println( e.toString() );
		}

		/* disconnect */
		try {
			if (!m_justShow)
				m_client.disconnect();
		} catch (Exception e) {
			System.err.println( e.toString() );
		}
		System.exit(0);
	} /* main */

	/**
	 * Prints usage.
	 */
    private static void doUsage() {
		System.err.println( "usage: LDAPModify [options]" );
		System.err.println("options");
		System.err.println("  -h host       LDAP server name or IP address");
		System.err.println("  -p port       LDAP server TCP port number");
		System.err.println("  -V version    LDAP protocol version " +
						   "number (default is 3)");
		System.err.println("  -D binddn     bind dn");
		System.err.println("  -w password   bind passwd (for simple " +
						   "authentication)");
		System.err.println("  -d level      set LDAP debugging level " +
						   "to \'level\'");
		System.err.println("  -R            do not automatically follow " +
						   "referrals");
		System.err.println("  -O hop limit  maximum number of referral " +
						   "hops to traverse");
		System.err.println("  -H            display usage information");
		System.err.println("  -c            continuous mode (do not " +
						   "stop on errors)");
		System.err.println("  -M            manage references (treat them " +
						   "as regular entries)");
		System.err.println("  -f file       read modifications from " +
						   "file instead of standard input");
		System.err.println("  -a            add entries");
		System.err.println("  -b            read values that start with " +
						   "/ from files (for bin attrs)");
		System.err.println("  -n            show what would be done but " +
						   "don\'t actually do it");
		System.err.println("  -v            run in verbose mode");
		System.err.println("  -r            replace existing values by " +
						   "default");
		System.err.println("  -e rejectfile save rejected entries in " +
						   "\'rejfile\'");
	}

	/**
	 * This class-method is used to extract specified parameters from the
	 * arguments list.
	 */
    /* extract parameters */
    protected static void extractParameters(String args[]) {

		String privateOpts = "abcHFre:f:";

		GetOpt options = LDAPTool.extractParameters( privateOpts, args );

		/* -H Help */
		if (options.hasOption('H')) {
			doUsage();
			System.exit(0);
		} /* Help */

		if (options.hasOption('F'))
			m_force = true;

		if (options.hasOption('a'))
			m_add = true;

		if (options.hasOption('c'))
			m_continuous = true;

		if (options.hasOption('r'))
			m_add = false;

		if (options.hasOption('b'))
			m_binaryFiles = true;

		/* -f input file */
		if(options.hasOption('f')) { /* Is input file */
			m_file = (String)options.getOptionParam('f');
		} /* End Is input file */

		/* -e rejects file */
		if(options.hasOption('e')) { /* rejects file */
			m_rejectsFile = (String)options.getOptionParam('e');
		} /* End rejects file */

	} /* extract parameters */

	/**
	 * This class-method is used to call the JDAP Modify Operation with the
	 * specified options, and/or parameters.
	 */
    private static void doModify() throws IOException { /* doModify */
		DataOutputStream reject = null;
		LDAPSearchConstraints cons = null;
		if (!m_justShow) {
			cons =
			  (LDAPSearchConstraints)m_client.getSearchConstraints().clone();
			if (m_ordinary) {
				LDAPControl control = new LDAPControl(
				  LDAPControl.MANAGEDSAIT, true, null);
				cons.setServerControls(control);
			}
        	cons.setReferrals( m_referrals );
			if ( m_referrals ) {
				setDefaultReferralCredentials( cons );
			}
        	cons.setHopLimit( m_hopLimit );
		}

		LDIFRecord rec = m_ldif.nextRecord();

		for (; rec != null; rec = m_ldif.nextRecord() ) {
			LDIFContent content = rec.getContent();
			LDAPModification mods[] = null;
			LDAPAttribute addAttrs[] = null;
			boolean doDelete = false;
			boolean doModDN = false;
			LDAPEntry newEntry = null;

			/* What type of record is this? */
			if ( content instanceof LDIFModifyContent ) {
				mods = ((LDIFModifyContent)content).getModifications();
			} else if ( content instanceof LDIFAddContent ) {
				addAttrs = ((LDIFAddContent)content).getAttributes();
			} else if ( content instanceof LDIFAttributeContent ) {
				/* No change type; decide what to do based on options */
                if ( m_add )
					addAttrs =
						((LDIFAttributeContent)content).getAttributes();
				else {
					LDAPAttribute[] tmpAttrs =
						((LDIFAttributeContent)content).getAttributes();
					mods = new LDAPModification[tmpAttrs.length];
					for( int ta = 0; ta < tmpAttrs.length; ta++ ) {
						mods[ta] = new LDAPModification(
							LDAPModification.REPLACE, tmpAttrs[ta] );
					}
				}
			} else if ( content instanceof LDIFDeleteContent ) {
				doDelete = true;
			} else if (content instanceof LDIFModDNContent ) {
				doModDN = true;
			} else {
			}

			/* Prepare for adding */
			if ( addAttrs != null ) {
				LDAPAttributeSet newAttrSet = new LDAPAttributeSet();
				for( int a = 0; a < addAttrs.length; a++ )
					newAttrSet.add( addAttrs[a] );
				newEntry = new LDAPEntry( rec.getDN(), newAttrSet );
			}

			/* Get values from files? */
			boolean skip = false;
			if ( m_binaryFiles ) {
				/* Check each value of each attribute, to see if it
				   needs replacing with the contents of a file */
				if ( mods != null ) {
					for( int m = 0; m < mods.length; m++ ) {
						LDAPModification mod = mods[m];
						LDAPAttribute attr = mods[m].getAttribute();

						LDAPAttribute newAttr = checkFiles( attr );
						if ( newAttr == null )
							skip = true;
						else
							mods[m] = new LDAPModification(
								mod.getOp(), newAttr );
					}
				} else if ( addAttrs != null ) {
					LDAPAttributeSet newAttrSet = new LDAPAttributeSet();
					for( int a = 0; a < addAttrs.length; a++ ) {
						LDAPAttribute attr = addAttrs[a];

						LDAPAttribute newAttr = checkFiles( attr );
						if ( newAttr == null ) {
							skip = true;
							break;
						} else {
							newAttrSet.add( newAttr );
						}
					}
					if ( !skip ) {
						newEntry = new LDAPEntry( rec.getDN(), newAttrSet );
					}
				}
			}

			/* Do the directory operation */
			int errCode = 0;
			if ( !skip ) {
				try {
					if ( mods != null ) {
						LDAPModificationSet modSet =
							new LDAPModificationSet();
						System.out.println("\nmodifying entry "+rec.getDN() );
						for( int m = 0; m < mods.length; m++ ) {
							if (m_verbose)
								System.out.println("\t"+mods[m] );
							modSet.add( mods[m].getOp(),
										mods[m].getAttribute() );
						}

						if (!m_justShow)
							m_client.modify( rec.getDN(), modSet, cons );
					} else if ( newEntry != null ) {
						System.out.println( "\nadding new entry " + newEntry.getDN() );
						if ( m_verbose ) {
							LDAPAttributeSet set = newEntry.getAttributeSet();
							for( int a = 0; a < set.size(); a++ ) {
								System.out.println("\t"+set.elementAt(a) );
							}
						} 
						if (!m_justShow)
							m_client.add( newEntry, cons );
					} else if ( doDelete ) {
						System.out.println( "\ndeleting entry " + rec.getDN() );
						if (!m_justShow)
							m_client.delete( rec.getDN(), cons );
					} else if ( doModDN) {
						System.out.println( "\nmodifying RDN of entry " + 
							rec.getDN()+" and/or moving it beneath a new parent");
						if ( m_verbose ) {
							System.out.println( "\t"+content.toString());
						}
						if (!m_justShow) {
							LDIFModDNContent moddnContent = (LDIFModDNContent)content;
							m_client.rename( rec.getDN(), moddnContent.getRDN(),
								moddnContent.getNewParent(), 
								moddnContent.getDeleteOldRDN(), cons );
							System.out.println( "rename completed");
						}
					}
				} catch (LDAPException e) {
					System.err.println( rec.getDN() + ": " +
					                    e.errorCodeToString() );
					if (e.getLDAPErrorMessage() != null)
						System.err.println( "additional info: " +
					                    e.getLDAPErrorMessage() );
					if ( !m_continuous )
						System.exit(1);
					skip = true;
					errCode = e.getLDAPResultCode();
				}
			}

			/* Write to rejects file? */
			if ( skip && (m_rejectsFile != null) ) {
				try {
					if ( reject == null ) {
						reject = new DataOutputStream(
							new FileOutputStream( m_rejectsFile ) );
					}
				} catch ( Exception e ) {
				}
				if ( reject != null ) {
					try {
						reject.writeUTF( "dn: "+rec.getDN()+ " # Error: " + errCode + '\n' );
						if ( mods != null ) {
							for( int m = 0; m < mods.length; m++ ) {
								reject.writeUTF( mods[m].toString() +
												   '\n' );
							}
						} else if ( newEntry != null ) {
							reject.writeUTF( "Add " + newEntry.toString()
											   + '\n' );
						} else if ( doDelete ) {
							reject.writeUTF( "Delete " + rec.getDN()
											   + '\n' );
						} else if (doModDN) {
							reject.writeUTF( "ModDN "+ 
							  ((LDIFModDNContent)content).toString()+'\n');
						}
					} catch ( IOException ex ) {
						System.err.println( ex.toString() );
						System.exit( 1 );
					}
				}
			}
		}
		System.exit(0);
	} /* doModify */


    /**
     * Read in binary data for values specified with a leading /
     * @param attr Source attribute.
     * @return Updated attribute.
     **/
    private static LDAPAttribute checkFiles ( LDAPAttribute attr ) {
		LDAPAttribute newAttr =
			new LDAPAttribute( attr.getName() );

		/* Check each value and see if it is a file name */
		Enumeration e = attr.getStringValues();
		if (e != null) {
		  while ( e.hasMoreElements() ) {
			String val = (String)e.nextElement();
			if ( (val != null) && (val.length() > 1)) {
				try {
					File file = new File( val );
					FileInputStream fi =
						new FileInputStream( file );
					byte[] bval = new byte[(int)file.length()];
					fi.read( bval, 0, (int)file.length() );
					newAttr.addValue( bval );
				} catch (FileNotFoundException ex) {
					newAttr.addValue(val) ;
				} catch ( IOException ex ) {
					System.err.println( "Unable to read value " +
						"from file " + val );
					if ( !m_continuous )
						System.exit(1);
					newAttr = null;
                }
			} else {
				newAttr.addValue( val );
			}
		  }
		}
		else
		  System.err.println("Failed to do string conversion for "+attr.getName());
		return newAttr;
	}

  private static boolean m_continuous = false;
  private static boolean m_force = false;
  private static boolean m_add = false;
  private static boolean m_binaryFiles = false;
  private static String m_rejectsFile = null;
  private static LDIF m_ldif = null;
  private static String m_file = null;
} /* LDAPModify */
