package org.opennms.netmgt.model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlIDREF;

import org.apache.commons.lang.builder.CompareToBuilder;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

@Entity
@Table(name = "linkstate")
public class OnmsLinkState implements Serializable, Comparable<OnmsLinkState> {
    
    public interface LinkStateTransition {
        public void onLinkUp();
        public void onLinkDown();
        public void onLinkUnknown();
    }

    private static final long serialVersionUID = 1L;

    public static enum LinkState {
        LINK_UP {
            @Override
            public LinkState nodeDown(LinkStateTransition transition) {
                transition.onLinkDown();
                return LINK_NODE_DOWN;
            }
            
            @Override
            public LinkState parentNodeDown(LinkStateTransition transition) {
                transition.onLinkDown();
                return LINK_PARENT_NODE_DOWN;
            }
            
            @Override
            public LinkState nodeEndPointDeleted(LinkStateTransition transition) {
                transition.onLinkUnknown();
                return LINK_NODE_UNMANAGED;
            }
            
            @Override
            public LinkState parentNodeEndPointDeleted(LinkStateTransition transition) {
                transition.onLinkUnknown();
                return LINK_PARENT_NODE_UNMANAGED;
            }

			@Override
			public String getDataLinkInterfaceStateType() {
				return "G";
			}
        },
        LINK_NODE_DOWN {
            @Override
            public LinkState nodeUp(LinkStateTransition transition) {
                transition.onLinkUp();
                return LINK_UP;
            }

            @Override
            public LinkState parentNodeDown(LinkStateTransition transition) {
                return LINK_BOTH_DOWN;
            }
            
            @Override
            public LinkState nodeEndPointDeleted(LinkStateTransition transition) {
                transition.onLinkUnknown();
                return LINK_NODE_UNMANAGED;
            }
            
            @Override
            public LinkState parentNodeEndPointDeleted(LinkStateTransition transition) {
                transition.onLinkUnknown();
                return LINK_PARENT_NODE_UNMANAGED;
            }

			@Override
			public String getDataLinkInterfaceStateType() {
				return "B";
			}
        },
        LINK_PARENT_NODE_DOWN {

            @Override
            public LinkState nodeDown(LinkStateTransition transition) {
                return LINK_BOTH_DOWN;
            }

            @Override
            public LinkState parentNodeUp(LinkStateTransition transition) {
                transition.onLinkUp();
                return LINK_UP;
            }
            
            @Override
            public LinkState nodeEndPointDeleted(LinkStateTransition transition) {
                transition.onLinkUnknown();
                return LINK_NODE_UNMANAGED;
            }
            
            @Override
            public LinkState parentNodeEndPointDeleted(LinkStateTransition transition) {
                transition.onLinkUnknown();
                return LINK_PARENT_NODE_UNMANAGED;
            }

			@Override
			public String getDataLinkInterfaceStateType() {
				return "B";
			}
            
        },
        LINK_BOTH_DOWN {

            @Override
            public LinkState nodeUp(LinkStateTransition transition) {
                return LINK_PARENT_NODE_DOWN;
            }

            @Override
            public LinkState parentNodeUp(LinkStateTransition transition) {
                return LINK_NODE_DOWN;
            }
            
            @Override
            public LinkState nodeEndPointDeleted(LinkStateTransition transition) {
                transition.onLinkUnknown();
                return LINK_NODE_UNMANAGED;
            }
            
            @Override
            public LinkState parentNodeEndPointDeleted(LinkStateTransition transition) {
                transition.onLinkUnknown();
                return LINK_PARENT_NODE_UNMANAGED;
            }

			@Override
			public String getDataLinkInterfaceStateType() {
				return "B";
			}
        },
        LINK_BOTH_UNMANAGED{
            @Override
            public LinkState nodeEndPointFound(LinkStateTransition transition) {
                return LINK_PARENT_NODE_UNMANAGED;
            }
            
            @Override
            public LinkState parentNodeEndPointFound(LinkStateTransition transition) {
                return LINK_NODE_UNMANAGED;
            }

			@Override
			public String getDataLinkInterfaceStateType() {
				return "U";
			}
        },
        LINK_PARENT_NODE_UNMANAGED{
          
            @Override
            public LinkState parentNodeEndPointFound(LinkStateTransition transition) {
                transition.onLinkUp();
                return LINK_UP;
            }
            
            @Override
            public LinkState nodeEndPointDeleted(LinkStateTransition transition) {
                return LINK_BOTH_UNMANAGED;
            }

			@Override
			public String getDataLinkInterfaceStateType() {
				return "U";
			}
             
            
        },
        LINK_NODE_UNMANAGED{
            
            @Override
            public LinkState parentNodeEndPointDeleted(LinkStateTransition transition) {
                return LINK_BOTH_UNMANAGED;
            }
            
            @Override
            public LinkState nodeEndPointFound(LinkStateTransition transition) {
                transition.onLinkUp();
                return LINK_UP;
            }

			@Override
			public String getDataLinkInterfaceStateType() {
				return "U";
			}
            
        };
        
        public abstract String getDataLinkInterfaceStateType();
        
        public LinkState nodeDown(LinkStateTransition transition) {
            return this;
        }
        public LinkState parentNodeDown(LinkStateTransition transition) {
            return this;
        }
        public LinkState nodeUp(LinkStateTransition transition) {
            return this;
        }
        public LinkState parentNodeUp(LinkStateTransition transition) {
            return this;
        }
        public LinkState down(boolean isParent, LinkStateTransition transition) {
            return isParent? parentNodeDown(transition) : nodeDown(transition);
        }
        public LinkState up(boolean isParent, LinkStateTransition transition) {
            return isParent? parentNodeUp(transition) : nodeUp(transition);
        }
        public LinkState nodeEndPointFound(LinkStateTransition transition) {
            return this;
        }
        public LinkState parentNodeEndPointFound(LinkStateTransition transition) {
            return this;
        }
        public LinkState parentNodeEndPointDeleted(LinkStateTransition transition) {
            return this;
            
        }
        public LinkState nodeEndPointDeleted(LinkStateTransition transition) {
            return this;
        }
    }

    private Integer m_id;
    private DataLinkInterface m_dataLinkInterface;
    private LinkState m_linkState = LinkState.LINK_UP;

    public OnmsLinkState() {
    }

    public OnmsLinkState(DataLinkInterface dataLinkInterface, LinkState linkState) {
        m_dataLinkInterface = dataLinkInterface;
        m_linkState = linkState;
    }

    @Id
    @SequenceGenerator(name="opennmsSequence", sequenceName="opennmsNxtId")
    @GeneratedValue(generator="opennmsSequence")
    public Integer getId() {
        return m_id;
    }

    public void setId(Integer id) {
        m_id = id;
    }
    
    @XmlIDREF
    @OneToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="datalinkinterfaceid")
    public DataLinkInterface getDataLinkInterface() {
        return m_dataLinkInterface;
    }

    public void setDataLinkInterface(DataLinkInterface dataLinkInterface) {
        m_dataLinkInterface = dataLinkInterface;
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "linkstate", length=24, nullable=false)
    public LinkState getLinkState() {
        return m_linkState;
    }
    
    public void setLinkState(LinkState linkState) {
        m_linkState = linkState;
    }

    public String toString() {
        return new ToStringBuilder(this)
            .append("id", getId())
            .append("datalink interface", getDataLinkInterface())
            .append("link state", getLinkState())
            .toString();
    }

    public boolean equals(Object o) {
        if (o instanceof OnmsLinkState) {
            OnmsLinkState lso = (OnmsLinkState) o;
            
            return new EqualsBuilder()
                .append(getId(), lso.getId())
                .append(getDataLinkInterface(), lso.getDataLinkInterface())
                .append(getLinkState(), lso.getLinkState())
                .isEquals();
        }
        return false;
    }
    
    public int compareTo(OnmsLinkState o) {
        return new CompareToBuilder()
            .append(getId(), o.getId())
            .append(getDataLinkInterface(), o.getDataLinkInterface())
            .append(getLinkState(), o.getLinkState())
            .toComparison();
    }
    
    public int hashCode() {
        return new HashCodeBuilder()
            .append(getId())
            .append(getDataLinkInterface())
            .append(getLinkState())
            .toHashCode();
    }
}
