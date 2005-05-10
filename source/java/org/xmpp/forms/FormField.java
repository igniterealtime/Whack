/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright 2004 Jive Software.
 *
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.xmpp.forms;

import org.dom4j.Element;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Represents a field of a form. The field could be used to represent a question to complete,
 * a completed question or a data returned from a search. The exact interpretation of the field
 * depends on the context where the field is used.
 *
 * @author Gaston Dombiak
 */
public class FormField {

    private Element element;

    FormField(Element element) {
        this.element = element;
    }

    /**
     * Adds a default value to the question if the question is part of a form to fill out.
     * Otherwise, adds an answered value to the question.
     *
     * @param value a default value or an answered value of the question.
     */
    public void addValue(String value) {
        element.addElement("value").setText(value);
    }

    /**
     * Removes all the values of the field.
     */
    public void clearValues() {
        for (Iterator it = element.elementIterator("value"); it.hasNext();) {
            it.next();
            it.remove();
        }
    }

    /**
     * Adds an available option to the question that the user has in order to answer
     * the question.
     *
     * @param label a label that represents the option.
     * @param value the value of the option.
     */
    public void addOption(String label, String value) {
        Element option = element.addElement("option");
        option.addAttribute("label", label);
        option.addElement("value").setText(value);
    }

    /**
     * Sets an indicative of the format for the data to answer. Valid formats are:
     * <p/>
     * <ul>
     * <li>text-single -> single line or word of text
     * <li>text-private -> instead of showing the user what they typed, you show ***** to
     * protect it
     * <li>text-multi -> multiple lines of text entry
     * <li>list-single -> given a list of choices, pick one
     * <li>list-multi -> given a list of choices, pick one or more
     * <li>boolean -> 0 or 1, true or false, yes or no. Default value is 0
     * <li>fixed -> fixed for putting in text to show sections, or just advertise your web
     * site in the middle of the form
     * <li>hidden -> is not given to the user at all, but returned with the questionnaire
     * <li>jid-single -> Jabber ID - choosing a JID from your roster, and entering one based
     * on the rules for a JID.
     * <li>jid-multi -> multiple entries for JIDs
     * </ul>
     *
     * @param type an indicative of the format for the data to answer.
     */
    public void setType(Type type) {
        element.addAttribute("type", type==null?null:type.toString());
    }

    /**
     * Sets the attribute that uniquely identifies the field in the context of the form. If the
     * field is of type "fixed" then the variable is optional.
     *
     * @param var the unique identifier of the field in the context of the form.
     */
    public void setVariable(String var) {
        element.addAttribute("var", var);
    }

    /**
     * Sets the label of the question which should give enough information to the user to
     * fill out the form.
     *
     * @param label the label of the question.
     */
    public void setLabel(String label) {
        element.addAttribute("label", label);
    }

    /**
     * Sets if the question must be answered in order to complete the questionnaire.
     *
     * @param required if the question must be answered in order to complete the questionnaire.
     */
    public void setRequired(boolean required) {
        // Remove an existing desc element.
        if (element.element("required") != null) {
            element.remove(element.element("required"));
        }
        if (required) {
            element.addElement("required");
        }
    }

    /**
     * Sets a description that provides extra clarification about the question. This information
     * could be presented to the user either in tool-tip, help button, or as a section of text
     * before the question.<p>
     * <p/>
     * If the question is of type FIXED then the description should remain empty.
     *
     * @param description provides extra clarification about the question.
     */
    public void setDescription(String description) {
        // Remove an existing desc element.
        if (element.element("desc") != null) {
            element.remove(element.element("desc"));
        }
        element.addElement("desc").setText(description);
    }

    /**
     * Returns true if the question must be answered in order to complete the questionnaire.
     *
     * @return true if the question must be answered in order to complete the questionnaire.
     */
    public boolean isRequired() {
        return element.element("required") != null;
    }

    /**
     * Returns the variable name that the question is filling out.
     *
     * @return the variable name of the question.
     */
    public String getVariable() {
        return element.attributeValue("var");
    }

    /**
     * Returns an Iterator for the default values of the question if the question is part
     * of a form to fill out. Otherwise, returns an Iterator for the answered values of
     * the question.
     *
     * @return an Iterator for the default values or answered values of the question.
     */
    public List<String> getValues() {
        List<String> answer = new ArrayList<String>();
        for (Iterator it = element.elementIterator("value"); it.hasNext();) {
            answer.add(((Element) it.next()).getTextTrim());
        }
        return answer;
    }

    /**
     * Returns an indicative of the format for the data to answer. Valid formats are:
     * <p/>
     * <ul>
     * <li>text-single -> single line or word of text
     * <li>text-private -> instead of showing the user what they typed, you show ***** to
     * protect it
     * <li>text-multi -> multiple lines of text entry
     * <li>list-single -> given a list of choices, pick one
     * <li>list-multi -> given a list of choices, pick one or more
     * <li>boolean -> 0 or 1, true or false, yes or no. Default value is 0
     * <li>fixed -> fixed for putting in text to show sections, or just advertise your web
     * site in the middle of the form
     * <li>hidden -> is not given to the user at all, but returned with the questionnaire
     * <li>jid-single -> Jabber ID - choosing a JID from your roster, and entering one based
     * on the rules for a JID.
     * <li>jid-multi -> multiple entries for JIDs
     * </ul>
     *
     * @return format for the data to answer.
     */
    public Type getType() {
        String type = element.attributeValue("type");
        if (type != null) {
            Type.valueOf(type);
        }
        return null;
    }

    /**
     * Returns the label of the question which should give enough information to the user to
     * fill out the form.
     *
     * @return label of the question.
     */
    public String getLabel() {
        return element.attributeValue("label");
    }

    /**
     * Returns a description that provides extra clarification about the question. This information
     * could be presented to the user either in tool-tip, help button, or as a section of text
     * before the question.<p>
     * <p/>
     * If the question is of type FIXED then the description should remain empty.
     *
     * @return description that provides extra clarification about the question.
     */
    public String getDescription() {
        return element.elementTextTrim("desc");
    }

    /**
     * Type-safe enumeration to represent the field type of the Data forms.
     */
    public enum Type {
        /**
         * The field enables an entity to gather or provide an either-or choice between two
         * options. The allowable values are 1 for yes/true/assent and 0 for no/false/decline.
         * The default value is 0.
         */
        boolean_type,

        /**
         * The field is intended for data description (e.g., human-readable text such as
         * "section" headers) rather than data gathering or provision. The <value/> child
         * SHOULD NOT contain newlines (the \n and \r characters); instead an application
         * SHOULD generate multiple fixed fields, each with one <value/> child.
         */
        fixed,

        /**
         * The field is not shown to the entity providing information, but instead is
         * returned with the form.
         */
        hidden,

        /**
         * The field enables an entity to gather or provide multiple Jabber IDs.
         */
        jid_multi,

        /**
         * The field enables an entity to gather or provide multiple Jabber IDs.
         */
        jid_single,

        /**
         * The field enables an entity to gather or provide one or more options from
         * among many.
         */
        list_multi,

        /**
         * The field enables an entity to gather or provide one option from among many.
         */
        list_single,

        /**
         * The field enables an entity to gather or provide multiple lines of text.
         */
        text_multi,

        /**
         * The field enables an entity to gather or provide a single line or word of text,
         * which shall be obscured in an interface (e.g., *****).
         */
        text_private,

        /**
         * The field enables an entity to gather or provide a single line or word of text,
         * which may be shown in an interface. This field type is the default and MUST be
         * assumed if an entity receives a field type it does not understand.
         */
        text_single;
    }
}
