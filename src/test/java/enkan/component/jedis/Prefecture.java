package enkan.component.jedis;

import java.io.Serializable;
import java.util.Objects;

public class Prefecture implements Serializable {
    private String code;
    private String name;

    @java.beans.ConstructorProperties({"code", "name"})
    public Prefecture(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public Prefecture() {
    }

    public String getCode() {
        return this.code;
    }

    public String getName() {
        return this.name;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof Prefecture)) return false;
        final Prefecture other = (Prefecture) o;
        if (!other.canEqual(this)) return false;
        final Object this$code = this.getCode();
        final Object other$code = other.getCode();
        if (!Objects.equals(this$code, other$code)) return false;
        final Object this$name = this.getName();
        final Object other$name = other.getName();
        if (!Objects.equals(this$name, other$name)) return false;
        return true;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $code = this.getCode();
        result = result * PRIME + ($code == null ? 43 : $code.hashCode());
        final Object $name = this.getName();
        result = result * PRIME + ($name == null ? 43 : $name.hashCode());
        return result;
    }

    protected boolean canEqual(Object other) {
        return other instanceof Prefecture;
    }

    public String toString() {
        return "Prefecture(code=" + this.getCode() + ", name=" + this.getName() + ")";
    }
}
