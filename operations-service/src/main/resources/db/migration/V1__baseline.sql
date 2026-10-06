--
-- PostgreSQL database dump
--


-- Dumped from database version 18.4
-- Dumped by pg_dump version 18.4

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: dlq_event; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.dlq_event (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    created_by character varying(255),
    updated_at timestamp(6) without time zone,
    updated_by character varying(255),
    final_error character varying(1000),
    merchant_id uuid NOT NULL,
    moved_at timestamp(6) without time zone,
    payload jsonb NOT NULL,
    replayed_at timestamp(6) without time zone,
    webhook_event_id uuid
);


--
-- Name: outbox_event; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.outbox_event (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    created_by character varying(255),
    updated_at timestamp(6) without time zone,
    updated_by character varying(255),
    aggregate_id uuid NOT NULL,
    aggregate_type character varying(255) NOT NULL,
    attempts integer NOT NULL,
    event_type character varying(50) NOT NULL,
    last_error character varying(1000),
    payload jsonb NOT NULL,
    published_at timestamp(6) without time zone,
    status character varying(255) NOT NULL,
    CONSTRAINT outbox_event_aggregate_type_check CHECK (((aggregate_type)::text = ANY ((ARRAY['PAYMENT'::character varying, 'ORDER'::character varying, 'REFUND'::character varying, 'SETTLEMENT'::character varying])::text[]))),
    CONSTRAINT outbox_event_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'PUBLISHED'::character varying, 'FAILED'::character varying])::text[])))
);


--
-- Name: settlement; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.settlement (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    created_by character varying(255),
    updated_at timestamp(6) without time zone,
    updated_by character varying(255),
    bank_reference character varying(50),
    failure_reason character varying(255),
    fee_amount_units integer NOT NULL,
    fee_amount_currency character varying(255) NOT NULL,
    gross_amount_units integer NOT NULL,
    gross_amount_currency character varying(255) NOT NULL,
    gst_amount_units integer NOT NULL,
    gst_amount_currency character varying(255) NOT NULL,
    merchant_id uuid NOT NULL,
    net_amount_units integer NOT NULL,
    net_amount_currency character varying(255) NOT NULL,
    processed_at timestamp(6) without time zone,
    refund_amount_units integer NOT NULL,
    refund_amount_currency character varying(255) NOT NULL,
    status character varying(20) NOT NULL,
    CONSTRAINT settlement_status_check CHECK (((status)::text = ANY ((ARRAY['INITIATED'::character varying, 'TRANSFER_PENDING'::character varying, 'PROCESSED'::character varying, 'FAILED'::character varying])::text[])))
);


--
-- Name: settlement_payment; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.settlement_payment (
    payment_id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    created_by character varying(255),
    updated_at timestamp(6) without time zone,
    updated_by character varying(255),
    settlement_id uuid NOT NULL
);


--
-- Name: webhook_event; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.webhook_event (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    created_by character varying(255),
    updated_at timestamp(6) without time zone,
    updated_by character varying(255),
    attempts integer NOT NULL,
    delivered_at timestamp(6) without time zone,
    event_type character varying(100) NOT NULL,
    last_attempt_at timestamp(6) without time zone,
    last_response_body character varying(1000),
    last_response_code integer,
    merchant_id uuid NOT NULL,
    next_retry_at timestamp(6) without time zone,
    payload jsonb,
    signature character varying(255) NOT NULL,
    status character varying(255) NOT NULL,
    target_url character varying(255) NOT NULL,
    CONSTRAINT webhook_event_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'DELIVERED'::character varying, 'FAILED'::character varying, 'DEAD'::character varying])::text[])))
);


--
-- Name: dlq_event dlq_event_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dlq_event
    ADD CONSTRAINT dlq_event_pkey PRIMARY KEY (id);


--
-- Name: outbox_event outbox_event_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.outbox_event
    ADD CONSTRAINT outbox_event_pkey PRIMARY KEY (id);


--
-- Name: settlement_payment settlement_payment_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.settlement_payment
    ADD CONSTRAINT settlement_payment_pkey PRIMARY KEY (payment_id, settlement_id);


--
-- Name: settlement settlement_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.settlement
    ADD CONSTRAINT settlement_pkey PRIMARY KEY (id);


--
-- Name: dlq_event uk2rnmlgvax847c3ws5yhtp1hgs; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dlq_event
    ADD CONSTRAINT uk2rnmlgvax847c3ws5yhtp1hgs UNIQUE (webhook_event_id);


--
-- Name: webhook_event webhook_event_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.webhook_event
    ADD CONSTRAINT webhook_event_pkey PRIMARY KEY (id);


--
-- Name: settlement_payment fk11x8ihxqap99rjvw65eitmmd; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.settlement_payment
    ADD CONSTRAINT fk11x8ihxqap99rjvw65eitmmd FOREIGN KEY (settlement_id) REFERENCES public.settlement(id);


--
-- Name: dlq_event fkqv72xi3tag231hjexxkni4q6a; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dlq_event
    ADD CONSTRAINT fkqv72xi3tag231hjexxkni4q6a FOREIGN KEY (webhook_event_id) REFERENCES public.webhook_event(id);


--
-- PostgreSQL database dump complete
--


