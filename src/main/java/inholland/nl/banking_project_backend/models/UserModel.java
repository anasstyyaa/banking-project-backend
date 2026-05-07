package inholland.nl.banking_project_backend.models;
import inholland.nl.banking_project_backend.enums.RoleEnum;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users")
@Data
public class UserModel implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String password;

    @Column(unique = true)
    private String email;
    private String firstName;
    private String lastName;
    private String bsn;
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    private RoleEnum role;

    @Column(nullable = false)
    private Boolean isApproved = false;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private CustomerProfileModel customerProfile;

    // Returns the Spring Security authorities granted to this user.
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.name()));
    }

    // Uses email as the login username for Spring Security.
    @Override
    public String getUsername() {
        return email;
    }

    // Keeps accounts valid unless future expiry rules are added.
    @Override
    public boolean isAccountNonExpired() { return true; }

    // Keeps accounts unlocked unless future lock rules are added.
    @Override
    public boolean isAccountNonLocked() { return true; }

    // Keeps credentials valid unless future expiry rules are added.
    @Override
    public boolean isCredentialsNonExpired() { return true; }

    // Keeps approved and unapproved users login-capable while services limit access.
    @Override
    public boolean isEnabled() { return true; }

}
