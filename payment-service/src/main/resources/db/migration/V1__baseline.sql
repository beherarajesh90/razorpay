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
-- Name: order_record; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.order_record (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    created_by character varying(255),
    updated_at timestamp(6) without time zone,
    updated_by character varying(255),
    amount_units integer,
    currency character varying(255),
    attempts integer NOT NULL,
    customer_id uuid,
    expires_at timestamp(6) without time zone NOT NULL,
    merchant_id uuid NOT NULL,
    notes jsonb,
    order_status character varying(20) NOT NULL,
    receipt character varying(100),
    CONSTRAINT order_record_order_status_check CHECK (((order_status)::text = ANY ((ARRAY['CREATED'::character varying, 'ATTEMPTED'::character varying, 'PAID'::character varying, 'CANCELLED'::character varying])::text[])))
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
-- Name: payment; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.payment (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    created_by character varying(255),
    updated_at timestamp(6) without time zone,
    updated_by character varying(255),
    amount_units integer,
    currency character varying(255),
    authorized_at timestamp(6) without time zone,
    bank_reference character varying(100),
    captured_at timestamp(6) without time zone,
    error_code character varying(100),
    error_description character varying(255),
    failed_at timestamp(6) without time zone,
    idempotency_key character varying(100) NOT NULL,
    merchant_id uuid NOT NULL,
    method smallint NOT NULL,
    method_details jsonb,
    processor_reference character varying(100),
    refunded_at timestamp(6) without time zone,
    settled_at timestamp(6) without time zone,
    status character varying(30) NOT NULL,
    order_id uuid NOT NULL,
    CONSTRAINT payment_method_check CHECK (((method >= 0) AND (method <= 3))),
    CONSTRAINT payment_status_check CHECK (((status)::text = ANY ((ARRAY['CREATED'::character varying, 'AUTHORIZING'::character varying, 'AUTHORIZED'::character varying, 'CAPTURING'::character varying, 'CAPTURED'::character varying, 'FAILED'::character varying, 'CANCELLED'::character varying, 'REFUNDED'::character varying, 'PARTIALLY_REFUNDED'::character varying, 'SETTLED'::character varying, 'AUTH_EXPIRED'::character varying])::text[])))
);


--
-- Name: payment_transition_log; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.payment_transition_log (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    created_by character varying(255),
    updated_at timestamp(6) without time zone,
    updated_by character varying(255),
    actor character varying(100),
    event character varying(30),
    from_status character varying(30),
    occured_at timestamp(6) without time zone NOT NULL,
    to_status character varying(30),
    payment_id uuid NOT NULL,
    CONSTRAINT payment_transition_log_actor_check CHECK (((actor)::text = ANY ((ARRAY['CUSTOMER'::character varying, 'MERCHANT'::character varying, 'SYSTEM'::character varying])::text[]))),
    CONSTRAINT payment_transition_log_event_check CHECK (((event)::text = ANY ((ARRAY['AUTHORIZE_ATTEMPT'::character varying, 'AUTHORIZE_SUCCESS'::character varying, 'AUTHORIZE_FAIL'::character varying, 'CAPTURE_REQUEST'::character varying, 'CAPTURE_SUCCESS'::character varying, 'CAPTURE_FAIL'::character varying, 'REFUND_INIT'::character varying, 'REFUND_COMPLETE'::character varying, 'SETTLE'::character varying, 'CANCEL'::character varying, 'CAPTURE_TIMEOUT'::character varying])::text[]))),
    CONSTRAINT payment_transition_log_from_status_check CHECK (((from_status)::text = ANY ((ARRAY['CREATED'::character varying, 'AUTHORIZING'::character varying, 'AUTHORIZED'::character varying, 'CAPTURING'::character varying, 'CAPTURED'::character varying, 'FAILED'::character varying, 'CANCELLED'::character varying, 'REFUNDED'::character varying, 'PARTIALLY_REFUNDED'::character varying, 'SETTLED'::character varying, 'AUTH_EXPIRED'::character varying])::text[]))),
    CONSTRAINT payment_transition_log_to_status_check CHECK (((to_status)::text = ANY ((ARRAY['CREATED'::character varying, 'AUTHORIZING'::character varying, 'AUTHORIZED'::character varying, 'CAPTURING'::character varying, 'CAPTURED'::character varying, 'FAILED'::character varying, 'CANCELLED'::character varying, 'REFUNDED'::character varying, 'PARTIALLY_REFUNDED'::character varying, 'SETTLED'::character varying, 'AUTH_EXPIRED'::character varying])::text[])))
);


--
-- Name: refund; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.refund (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    created_by character varying(255),
    updated_at timestamp(6) without time zone,
    updated_by character varying(255),
    amount_units integer,
    currency character varying(255),
    bank_reference character varying(100),
    error_code character varying(100),
    error_description character varying(500),
    idempotency_key character varying(100) NOT NULL,
    merchant_id uuid NOT NULL,
    notes jsonb,
    processed_at timestamp(6) without time zone,
    status character varying(255) NOT NULL,
    payment_id uuid NOT NULL,
    CONSTRAINT refund_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'PROCESSING'::character varying, 'PROCESSED'::character varying, 'FAILED'::character varying])::text[])))
);


--
-- Name: order_record order_record_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.order_record
    ADD CONSTRAINT order_record_pkey PRIMARY KEY (id);


--
-- Name: outbox_event outbox_event_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.outbox_event
    ADD CONSTRAINT outbox_event_pkey PRIMARY KEY (id);


--
-- Name: payment payment_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payment
    ADD CONSTRAINT payment_pkey PRIMARY KEY (id);


--
-- Name: payment_transition_log payment_transition_log_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payment_transition_log
    ADD CONSTRAINT payment_transition_log_pkey PRIMARY KEY (id);


--
-- Name: refund refund_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.refund
    ADD CONSTRAINT refund_pkey PRIMARY KEY (id);


--
-- Name: refund uk_refund_merchant_idempotency_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.refund
    ADD CONSTRAINT uk_refund_merchant_idempotency_key UNIQUE (merchant_id, idempotency_key);


--
-- Name: idx_order_id_merchant_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_order_id_merchant_id ON public.order_record USING btree (id, merchant_id);


--
-- Name: idx_order_merchant_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_order_merchant_id ON public.order_record USING btree (merchant_id);


--
-- Name: idx_payment_merchant_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_payment_merchant_id ON public.payment USING btree (merchant_id);


--
-- Name: idx_payment_order_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_payment_order_id ON public.payment USING btree (order_id);


--
-- Name: idx_payment_transition_log_payment_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_payment_transition_log_payment_id ON public.payment_transition_log USING btree (payment_id);


--
-- Name: payment fk8tny818kg1ue5ajkn040ed8lm; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payment
    ADD CONSTRAINT fk8tny818kg1ue5ajkn040ed8lm FOREIGN KEY (order_id) REFERENCES public.order_record(id);


--
-- Name: refund fkeoh1147brjy6m009cswl5lty4; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.refund
    ADD CONSTRAINT fkeoh1147brjy6m009cswl5lty4 FOREIGN KEY (payment_id) REFERENCES public.payment(id);


--
-- Name: payment_transition_log fki15m2a3cnw0l7p1jfc1pvx15a; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payment_transition_log
    ADD CONSTRAINT fki15m2a3cnw0l7p1jfc1pvx15a FOREIGN KEY (payment_id) REFERENCES public.payment(id);


--
-- PostgreSQL database dump complete
--


