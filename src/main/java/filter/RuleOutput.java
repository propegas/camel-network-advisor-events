
package filter;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
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
 *         &lt;element ref="{}statement"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {"statement"})
@XmlRootElement(name = "ruleOutput")
public class RuleOutput {

    @XmlElement(required = true)
    protected Statement statement;

    /**
     * Gets the value of the statement property.
     *
     * @return possible object is
     * {@link Statement }
     */
    public Statement getStatement() {
        return statement;
    }

    /**
     * Sets the value of the statement property.
     *
     * @param value allowed object is
     *              {@link Statement }
     */
    public void setStatement(Statement value) {
        this.statement = value;
    }

}
