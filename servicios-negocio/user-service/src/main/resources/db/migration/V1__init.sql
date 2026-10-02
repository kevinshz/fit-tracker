
    create table user_profiles (
        body_weight float(53),
        date_of_birth date,
        height float(53),
        created_at timestamp(6) not null,
        updated_at timestamp(6) not null,
        id uuid not null,
        user_id uuid not null unique,
        gender varchar(20),
        experience_level varchar(30),
        fitness_goal varchar(50),
        name varchar(100) not null,
        email varchar(150) not null unique,
        primary key (id)
    );
