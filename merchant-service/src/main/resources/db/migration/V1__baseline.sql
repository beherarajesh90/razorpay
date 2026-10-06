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
-- Name: api_key; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.api_key (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    created_by character varying(255),
    updated_at timestamp(6) without time zone,
    updated_by character varying(255),
    enabled boolean NOT NULL,
    environment character varying(10) NOT NULL,
    grace_period_expires_at timestamp(6) without time zone,
    key_id character varying(50) NOT NULL,
    key_secret_hash character varying(200) NOT NULL,
    last_used_at timestamp(6) without time zone,
    previous_key_secret_hash character varying(200),
    rotated_at timestamp(6) without time zone,
    merchant_id uuid NOT NULL,
    CONSTRAINT api_key_environment_check CHECK (((environment)::text = ANY ((ARRAY['LIVE'::character varying, 'TEST'::character varying])::text[])))
);


--
-- Name: app_user; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.app_user (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    created_by character varying(255),
    updated_at timestamp(6) without time zone,
    updated_by character varying(255),
    email character varying(255) NOT NULL,
    password_hash character varying(255) NOT NULL,
    role character varying(255) NOT NULL,
    merchant_id uuid,
    CONSTRAINT app_user_role_check CHECK (((role)::text = ANY ((ARRAY['OWNER'::character varying, 'ADMIN'::character varying, 'TEAM'::character varying])::text[])))
);


--
-- Name: customer; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.customer (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    created_by character varying(255),
    updated_at timestamp(6) without time zone,
    updated_by character varying(255),
    deleted_at timestamp(6) without time zone,
    email character varying(200),
    name character varying(200),
    phone character varying(20),
    merchant_id uuid NOT NULL
);


--
-- Name: merchant; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.merchant (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    created_by character varying(255),
    updated_at timestamp(6) without time zone,
    updated_by character varying(255),
    business_name character varying(100),
    business_type character varying(50),
    contact_number character varying(20),
    email character varying(255) NOT NULL,
    gst_id character varying(20),
    name character varying(200) NOT NULL,
    pan_id character varying(20),
    settlement_bank_account character varying(200),
    settlement_bank_account_holder_name character varying(200),
    settlement_bank_ifsc character varying(20),
    status character varying(200) NOT NULL,
    website_url character varying(200),
    CONSTRAINT merchant_business_type_check CHECK (((business_type)::text = ANY ((ARRAY['LLP'::character varying, 'PROPRIETORSHIP'::character varying, 'PARTNERSHIP'::character varying, 'PRIVATE_LIMITED'::character varying, 'PUBLIC_LIMITED'::character varying, 'TRUST'::character varying])::text[]))),
    CONSTRAINT merchant_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING_KYC'::character varying, 'ACTIVE'::character varying, 'SUSPENDED'::character varying])::text[])))
);


--
-- Name: merchant_webhook_config; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.merchant_webhook_config (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    created_by character varying(255),
    updated_at timestamp(6) without time zone,
    updated_by character varying(255),
    enabled boolean NOT NULL,
    event_types character varying(255),
    target_url character varying(500) NOT NULL,
    webhook_secret character varying(255),
    merchant_id uuid NOT NULL
);


--
-- Name: api_key api_key_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.api_key
    ADD CONSTRAINT api_key_pkey PRIMARY KEY (id);


--
-- Name: app_user app_user_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.app_user
    ADD CONSTRAINT app_user_pkey PRIMARY KEY (id);


--
-- Name: customer customer_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customer
    ADD CONSTRAINT customer_pkey PRIMARY KEY (id);


--
-- Name: merchant merchant_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.merchant
    ADD CONSTRAINT merchant_pkey PRIMARY KEY (id);


--
-- Name: merchant_webhook_config merchant_webhook_config_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.merchant_webhook_config
    ADD CONSTRAINT merchant_webhook_config_pkey PRIMARY KEY (id);


--
-- Name: app_user uk1j9d9a06i600gd43uu3km82jw; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.app_user
    ADD CONSTRAINT uk1j9d9a06i600gd43uu3km82jw UNIQUE (email);


--
-- Name: merchant uk22hw5xdmw9ehbp92kr3h9pbh; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.merchant
    ADD CONSTRAINT uk22hw5xdmw9ehbp92kr3h9pbh UNIQUE (email);


--
-- Name: api_key uk4rx8a3gpjkagf3diw254x2ery; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.api_key
    ADD CONSTRAINT uk4rx8a3gpjkagf3diw254x2ery UNIQUE (key_id);


--
-- Name: idx_api_key_merchant_env; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_api_key_merchant_env ON public.api_key USING btree (merchant_id, environment, enabled);


--
-- Name: idx_app_user_merchant_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_app_user_merchant_id ON public.app_user USING btree (merchant_id);


--
-- Name: idx_customer_email; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_customer_email ON public.customer USING btree (email);


--
-- Name: idx_merchant_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_merchant_status ON public.merchant USING btree (status);


--
-- Name: idx_webhook_merchant_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_webhook_merchant_id ON public.merchant_webhook_config USING btree (merchant_id, enabled);


--
-- Name: api_key fke8e15uritb2pto9w7hepoxqor; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.api_key
    ADD CONSTRAINT fke8e15uritb2pto9w7hepoxqor FOREIGN KEY (merchant_id) REFERENCES public.merchant(id);


--
-- Name: customer fkegoqw2qeun0mllg61gt5jdd6h; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customer
    ADD CONSTRAINT fkegoqw2qeun0mllg61gt5jdd6h FOREIGN KEY (merchant_id) REFERENCES public.merchant(id);


--
-- Name: app_user fkjksmfgh6vcrbc0fow9oecdamf; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.app_user
    ADD CONSTRAINT fkjksmfgh6vcrbc0fow9oecdamf FOREIGN KEY (merchant_id) REFERENCES public.merchant(id);


--
-- Name: merchant_webhook_config fkt4q07s1ff5ifr43s1u9vmfb5f; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.merchant_webhook_config
    ADD CONSTRAINT fkt4q07s1ff5ifr43s1u9vmfb5f FOREIGN KEY (merchant_id) REFERENCES public.merchant(id);


--
-- PostgreSQL database dump complete
--


