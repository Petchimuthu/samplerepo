/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
*/
package com.adventnet.pmd.rules;

//import net.sourceforge.pmd.lang.rule.AbstractRule;
import net.sourceforge.pmd.lang.java.rule.AbstractJavaRule;
import net.sourceforge.pmd.lang.java.ast.ASTBlock;
import net.sourceforge.pmd.lang.java.ast.ASTClassOrInterfaceType;
import net.sourceforge.pmd.lang.java.ast.ASTLocalVariableDeclaration;
import net.sourceforge.pmd.lang.java.ast.ASTMethodDeclaration;
import net.sourceforge.pmd.lang.java.ast.ASTName;
import net.sourceforge.pmd.lang.java.ast.ASTPrimitiveType;
import net.sourceforge.pmd.lang.java.ast.ASTReferenceType;
import net.sourceforge.pmd.lang.java.ast.ASTReturnStatement;
import net.sourceforge.pmd.lang.java.ast.ASTTryStatement;
import net.sourceforge.pmd.lang.java.ast.ASTType;
import net.sourceforge.pmd.lang.java.ast.ASTVariableDeclarator;
import net.sourceforge.pmd.lang.java.ast.ASTVariableDeclaratorId;
import net.sourceforge.pmd.lang.ast.Node;

//iv
import net.sourceforge.pmd.lang.java.ast.ASTPrimaryExpression;
import net.sourceforge.pmd.lang.java.ast.ASTPrimaryPrefix;
import net.sourceforge.pmd.lang.java.ast.ASTPrimarySuffix;
import net.sourceforge.pmd.lang.java.ast.ASTArguments;
import net.sourceforge.pmd.lang.java.ast.ASTArgumentList;
import net.sourceforge.pmd.lang.java.ast.ASTMethodDeclarator;
import net.sourceforge.pmd.lang.java.ast.ASTFinallyStatement;
import net.sourceforge.pmd.lang.java.ast.ASTFormalParameters;
import net.sourceforge.pmd.lang.java.ast.ASTFormalParameter;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

import java.util.Hashtable;
import net.sourceforge.pmd.lang.java.ast.ASTClassOrInterfaceDeclaration;
import net.sourceforge.pmd.lang.rule.properties.StringProperty;

/**
 * Makes sure you close your Streams. It does this by
 * looking for code patterned like this:
 * <pre>
 *  FileInputStream fis = null;
 *  try {
 *   // do stuff, and maybe catch something
 *  } finally {
 *   fis.close();
 *  }
 * </pre>
 */
public class StreamClose extends AbstractJavaRule
{

	public Hashtable checkEntriesTable = null;
        public Hashtable methodNamesTable = null;
	private Vector getStreamNames()
        {
		String streamNames = getProperty(new StringProperty("checkForStreams", "XPath expression", "", 1.0f));
                
                //System.out.println("Stream Names from xml file----->  " +streamNames);

		StringTokenizer st = new StringTokenizer(streamNames,",");

		Vector v = new Vector();
		while(st.hasMoreTokens())
		{
			v.add((String)st.nextToken());
                        int vectsize = v.size();
                        //System.out.println("Vector Size----> "+vectsize);
		}
		return v;
	}


	public Object visit(ASTMethodDeclaration node, Object data)
	{
            //List vars = node.findChildrenOfType(ASTLocalVariableDeclaration.class);
            List vars = node.findDescendantsOfType(ASTLocalVariableDeclaration.class);
            List ids = new ArrayList();
            List streamsFound = new ArrayList();
            Vector streamvector = getStreamNames();
            //System.out.println("Get Stream Names------>  "+streamvector);
            Hashtable variablesTable = new Hashtable();
            
        
        // find all the open streams within the method
            for (Iterator it = vars.iterator(); it.hasNext();)
            {
                ASTLocalVariableDeclaration var = (ASTLocalVariableDeclaration) it.next();
                ASTType type = var.getTypeNode();
          
               //System.out.println("Inside for");           
     
                if (type.jjtGetChild(0) instanceof ASTReferenceType)
                {
               //System.out.println("Inside If");           
                    ASTReferenceType astreftype = (ASTReferenceType)type.jjtGetChild(0);
                    if(astreftype.jjtGetChild(0) instanceof ASTClassOrInterfaceType)
                    {
                        ASTClassOrInterfaceType astclass = (ASTClassOrInterfaceType)astreftype.jjtGetChild(0);
                        String imgname = astclass.getImage();                                                
                        System.out.println("Image Name from astclass------>  "+imgname);
                        if(streamvector.contains(imgname))
                        {
                            List localdecls = var.findDescendantsOfType(ASTVariableDeclarator.class);                            
                            for(Iterator localvars = localdecls.iterator(); localvars.hasNext(); )
                            {
                                ASTVariableDeclarator vardecl = (ASTVariableDeclarator) localvars.next();
                                ASTVariableDeclaratorId id = (ASTVariableDeclaratorId) vardecl.jjtGetChild(0);
                                ids.add(id);
                                streamsFound.add(imgname);
                            }                            
                        }
                    }
                }
            }

        // if there are open streams, ensure each is closed.
            for (int i = 0; i < ids.size(); i++)
            {
                ASTVariableDeclaratorId x = (ASTVariableDeclaratorId) ids.get(i);
                ensureClosed((ASTLocalVariableDeclaration) x.jjtGetParent().jjtGetParent(), x, data);
            }
               
            return data;
	}

	 private void ensureClosed(ASTLocalVariableDeclaration var,ASTVariableDeclaratorId id, Object data)
	 {
		 // What are the chances of a Connection being instantiated in a
		 // for-loop init block? Anyway, I'm lazy!
		 String idName = id.getImage();
		 String target = idName + ".close";

                 Node n = var;
                 
                 while (!((n = n.jjtGetParent()) instanceof ASTBlock));
                 
                 ASTBlock top = (ASTBlock) n;

                 /*
                  * Looking if the stream is returned from the method.
                  * */

                 List<ASTReturnStatement> returnStatements = top.findDescendantsOfType(ASTReturnStatement.class);
                 int numOfReturns = returnStatements.size();

                 for(int j = 0;j<numOfReturns;j++){
                    List<ASTName> returnVarName =  returnStatements.get(j).findDescendantsOfType(ASTName.class);
                    int returnVarLenth = returnVarName.size();

                    if(returnVarLenth == 0){
                        continue;
                    }

                    for(int k=0;k<returnVarLenth;k++){
                        String returnVariable = returnVarName.get(k).getImage();
                        if(returnVariable.equals(idName)){
                            return;
                        }
                    }
                 }

                 /* look for try blocks below the line the variable was
                  introduced and make sure there is a .close call in a finally
                  block.*/

                 List tryblocks = new Vector();
                 tryblocks = top.findDescendantsOfType(ASTTryStatement.class);
                 System.out.println("TRY"+ top.findDescendantsOfType(ASTTryStatement.class)); 
                 for (Iterator it = tryblocks.iterator(); it.hasNext();)
                 {
                     
                     ASTFinallyStatement finallyBlock;
                     ASTTryStatement t = (ASTTryStatement) it.next();
                     if ((t.getBeginLine() > id.getBeginLine()) && (t.hasFinally()))
                     {
                         finallyBlock = t.getFinally();
                         List names = new ArrayList();
                         names = finallyBlock.findDescendantsOfType(ASTName.class);
                         System.out.println("NAMES"+names); 
                         for (Iterator it2 = names.iterator(); it2.hasNext();)
                         {
                             ASTName ASTDataValue1=((ASTName) it2.next());
                             String ASTDataValue=ASTDataValue1.getImage();
                             System.out.println("ASTDATA" + ASTDataValue + "  " + ASTDataValue1);
                             if (ASTDataValue.equals(target))
                             {
                                 return;
                             }
                         }

                        /*
                         * Looking if the stream is passed as argument for any method.
                         * */

                         List arguments = new Vector();
			List pnames = new ArrayList();
			arguments = finallyBlock.findDescendantsOfType(ASTArguments.class);
			for (Iterator it3 = arguments.iterator(); it3.hasNext();)
			{
			        ASTArguments argument = (ASTArguments)it3.next();
			        pnames = argument.findDescendantsOfType(ASTName.class);
			        for (Iterator it4 = pnames.iterator(); it4.hasNext();)
			        {
			       	 ASTName ASTArgValue1=((ASTName) it4.next());
			       	 String ASTArgValue=ASTArgValue1.getImage();
                                 System.out.println("ASTARG" + ASTArgValue + "  " + idName);
			       	 if (ASTArgValue.equals(idName))
			       	 {
			       		 return;
			       	 }
			        }
			}

                     }
                 }
                         
                 // if all is not well, complain
                     int errline = id.getBeginLine();
                     String sname = "";
                     try
                     {
                          sname = ( (ASTClassOrInterfaceType)var.getFirstDescendantOfType(Class.forName("net.sourceforge.pmd.lang.java.ast.ASTClassOrInterfaceType")) ).getImage();
                     }
                     catch(Exception e)
                     {
                        e.printStackTrace();
                     }
                     String vname = id.getImage();
                     String errorMsg = "Stream " + sname + " must be properly closed in the finally block";
                     addViolationWithMessage(data, var, errorMsg);
   }
}
