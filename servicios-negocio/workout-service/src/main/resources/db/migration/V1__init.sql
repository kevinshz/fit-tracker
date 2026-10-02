
    create table workout_session (
        performed_at date not null,
        created_at timestamp(6) not null,
        updated_at timestamp(6) not null,
        id uuid not null,
        user_id uuid not null,
        session_type varchar(20) not null check (session_type in ('STRENGTH','HYPERTROPHY')),
        muscle_label varchar(30) not null check (muscle_label in ('CHEST_BACK','LEGS','PUSH','PULL','SHOULDERS_ARMS','FULL_BODY')),
        notes varchar(500),
        primary key (id)
    );

    create table workout_set (
        reps integer not null,
        rir integer,
        set_number integer not null,
        volume_kg numeric(10,2) not null,
        weight_kg numeric(6,2) not null,
        created_at timestamp(6) not null,
        exercise_id uuid not null,
        id uuid not null,
        session_id uuid not null,
        exercise_name varchar(200) not null,
        primary key (id)
    );

    create index idx_ws_user_date 
       on workout_session (user_id, performed_at);

    create index idx_ws_user_label 
       on workout_session (user_id, muscle_label);

    create index idx_ws_label_type 
       on workout_session (muscle_label, session_type);

    create index idx_wset_session 
       on workout_set (session_id);

    create index idx_wset_exercise 
       on workout_set (exercise_id);

    alter table if exists workout_set 
       add constraint FKlc9s0673svdp19ip133fdcq9u 
       foreign key (session_id) 
       references workout_session;
