
package filter;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>Java class for anonymous complex type.
 * <p/>
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p/>
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element ref="{}ruleInput"/>
 *         &lt;element ref="{}ruleOutput"/>
 *       &lt;/sequence>
 *       &lt;attribute name="id" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {"ruleInput", "ruleOutput"})
@XmlRootElement(name = "enrichRules")
public class EnrichRules {

    @XmlElement(required = true)
    protected RuleInput ruleInput;
    @XmlElement(required = true)
    protected RuleOutput ruleOutput;
    @XmlAttribute(name = "id")
    protected String id;

    /**
     * Description.
     * <pre><![CDATA[/^.*$/gs]]></pre>
     *
     * @return possible object is
     * {@link RuleInput }
     */
    public RuleInput getRuleInput() {
        return ruleInput;
    }

    /**
     * Sets the value of the ruleInput property.
     *
     * @param value allowed object is
     *              {@link RuleInput }
     */
    public void setRuleInput(RuleInput value) {
        this.ruleInput = value;
    }

    /**
     * Gets the value of the ruleOutput property.
     *
     * @return possible object is
     * {@link RuleOutput }
     */
    public RuleOutput getRuleOutput() {
        return ruleOutput;
    }

    /**
     * Sets the value of the ruleOutput property.
     *
     * @param value allowed object is
     *              {@link RuleOutput }
     */
    public void setRuleOutput(RuleOutput value) {
        this.ruleOutput = value;
    }

    /**
     * Gets the value of the id property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the value of the id property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setId(String value) {
        this.id = value;
    }

}
