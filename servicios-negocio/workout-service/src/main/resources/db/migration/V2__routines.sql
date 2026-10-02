
    alter table workout_session add column routine_name varchar(100);

    create table workout_routine (
        created_at timestamp(6) not null,
        updated_at timestamp(6) not null,
        id uuid not null,
        user_id uuid not null,
        name varchar(100) not null,
        session_type varchar(20) check (session_type in ('STRENGTH','HYPERTROPHY')),
        muscle_label varchar(30) check (muscle_label in ('CHEST_BACK','LEGS','PUSH','PULL','SHOULDERS_ARMS','FULL_BODY')),
        notes varchar(500),
        primary key (id)
    );

    create table workout_routine_exercise (
        position integer not null,
        planned_sets integer not null,
        id uuid not null,
        routine_id uuid not null,
        exercise_id uuid not null,
        exercise_name varchar(200) not null,
        primary key (id)
    );

    create table workout_session_exercise (
        position integer not null,
        planned_sets integer not null,
        id uuid not null,
        session_id uuid not null,
        exercise_id uuid not null,
        exercise_name varchar(200) not null,
        primary key (id)
    );

    create index idx_wr_user
       on workout_routine (user_id);

    create index idx_wre_routine
       on workout_routine_exercise (routine_id);

    create index idx_wse_session
       on workout_session_exercise (session_id);

    alter table if exists workout_routine_exercise
       add constraint FKwre_routine
       foreign key (routine_id)
       references workout_routine;

    alter table if exists workout_session_exercise
       add constraint FKwse_session
       foreign key (session_id)
       references workout_session;
