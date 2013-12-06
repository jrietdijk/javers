package org.javers.model.mapping;

import org.javers.common.validation.Validate;
import org.javers.core.exceptions.JaversException;
import org.javers.core.exceptions.JaversExceptionCode;
import org.javers.model.mapping.type.JaversType;
import org.javers.model.mapping.type.TypeMapper;
import java.util.HashSet;
import java.util.Set;

import static org.javers.common.validation.Validate.argumentsAreNotNull;

/**
 * EntityManager bootstrap is two-phased:
 * <ol>
 *     <li/>JaVers bootstrap should
 *          registering client's Entities and ValueObjects through {@link #register(ManagedClassDefinition)}.
 *          In this phase, EntityManager creates proper {@link JaversType}'s in {@link TypeMapper}.
 *     <li/>When all types are registered, JaVers bootstrap calls {@link #buildManagedClasses()},
 *          in order to create Entities and ValueObjects for all previously registered types.
 * </ol>
 *
 * @author bartosz walacik
 */
public class EntityManager {

    private final EntityFactory entityFactory;
    private final TypeMapper typeMapper;

    private final Set<ManagedClassDefinition> managedClassDefinitions = new HashSet<>();
    private final ManagedClasses managedClasses = new ManagedClasses();

    public EntityManager(EntityFactory entityFactory, TypeMapper typeMapper) {
        argumentsAreNotNull(entityFactory, typeMapper);

        this.entityFactory = entityFactory;
        this.typeMapper = typeMapper;
    }

    /**
     * @throws JaversException if class is not managed
     */
    public ManagedClass getByClass(Class<?> clazz) {
        if (!isRegistered(clazz)) {
            throw new JaversException(JaversExceptionCode.CLASS_NOT_MANAGED, clazz.getName());
        }
        if (isRegistered(clazz) && !isManaged(clazz)) {
            throw new JaversException(JaversExceptionCode.ENTITY_MANAGER_NOT_INITIALIZED, clazz.getName());
        }
        return managedClasses.getBySourceClass(clazz);
    }

    public void register(ManagedClassDefinition def) {
        Validate.argumentIsNotNull(def);

        if (isRegistered(def)) {
            return; //already managed
        }

        if (def instanceof EntityDefinition) {
            typeMapper.registerEntityReferenceType(def.getClazz());
        }
        if (def instanceof  ValueObjectDefinition) {
            typeMapper.registerValueObjectType(def.getClazz());
        }
        managedClassDefinitions.add(def);
    }

    private boolean isRegistered(ManagedClassDefinition def) {
        return managedClassDefinitions.contains(def);
    }

    private boolean isRegistered(Class clazz) {
        //TODO optimize this lame loop
        for (ManagedClassDefinition def : managedClassDefinitions) {
            if (def.getClazz() == clazz){
                return true;
            }
        }
        return false;
    }

    public boolean isManaged(Class<?> clazz) {
        return managedClasses.containsManagedClassWithSourceClass(clazz);
    }

    /**
     * EntityManager is up & ready after calling {@link #buildManagedClasses()}
     */
    public boolean isInitialized() {
        return managedClasses.count() == typeMapper.getCountOfEntitiesAndValueObjects();
    }

    /**
     * call that if all Entities and ValueObject are registered
     */
    public void buildManagedClasses() {
        for (ManagedClassDefinition def : managedClassDefinitions) {
            if (def instanceof  EntityDefinition) {
                manageEntity((EntityDefinition)def);
            }
            if (def instanceof  ValueObjectDefinition) {
                manageValueObject((ValueObjectDefinition)def);
            }
        }
    }

    private void manageEntity(EntityDefinition entityDef) {
        managedClasses.add(entityFactory.create(entityDef));
    }

    private void manageValueObject(ValueObjectDefinition voDef) {
        managedClasses.add(new ValueObject(voDef.getClazz()));
    }
}
