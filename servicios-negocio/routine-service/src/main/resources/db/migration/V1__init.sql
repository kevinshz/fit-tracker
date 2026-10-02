
    create table exercise (
        is_custom boolean not null,
        id uuid not null,
        type varchar(20) not null check (type in ('STRENGTH','CARDIO','MOBILITY')),
        equipment varchar(30) not null check (equipment in ('BARBELL','DUMBBELL','MACHINE','CABLE','BODY_WEIGHT','KETTLEBELL','BANDS','MEDICINE_BALL','EXERCISE_BALL','EZ_CURL_BAR','FOAM_ROLL','OTHER')),
        primary_muscle varchar(30) not null check (primary_muscle in ('ABS','ABDUCTORS','ADDUCTORS','BICEPS','CALVES','CHEST','FOREARMS','GLUTES','HAMSTRINGS','LATS','LOWER_BACK','MIDDLE_BACK','NECK','QUADRICEPS','SHOULDERS','TRAPS','TRICEPS','OTHER')),
        progression_strategy varchar(30) not null check (progression_strategy in ('LINEAR','DOUBLE_PROGRESSION')),
        name varchar(200) not null,
        gif_url varchar(300),
        image_url varchar(300),
        attribution varchar(500),
        created_by varchar(255),
        external_id varchar(255),
        instructions_en text,
        instructions_es text,
        primary key (id),
        constraint uk_exercise_name unique (name)
    );

    create table exercise_alias (
        exercise_id uuid not null,
        id uuid not null,
        alias varchar(200) not null,
        primary key (id),
        constraint uk_alias unique (alias)
    );

    create table exercise_secondary_muscles (
        exercise_id uuid not null,
        muscle varchar(30) check (muscle in ('ABS','ABDUCTORS','ADDUCTORS','BICEPS','CALVES','CHEST','FOREARMS','GLUTES','HAMSTRINGS','LATS','LOWER_BACK','MIDDLE_BACK','NECK','QUADRICEPS','SHOULDERS','TRAPS','TRICEPS','OTHER'))
    );

    create index idx_exercise_external_id 
       on exercise (external_id);

    create index idx_exercise_primary_muscle 
       on exercise (primary_muscle);

    create index idx_exercise_equipment 
       on exercise (equipment);

    create index idx_alias 
       on exercise_alias (alias);

    alter table if exists exercise_alias 
       add constraint FK268lvc0d5nq2sg50l8exruhio 
       foreign key (exercise_id) 
       references exercise;

    alter table if exists exercise_secondary_muscles 
       add constraint FKfmc6n3e0yosbeqacew1wu4m2x 
       foreign key (exercise_id) 
       references exercise;
