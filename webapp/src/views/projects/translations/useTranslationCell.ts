import React, { useEffect } from 'react';
import { getTolgeeFormat } from '@tginternal/editor';

import { TRANSLATION_STATES } from 'tg.constants/translationStates';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';
import { components } from 'tg.service/apiSchema.generated';

import {
  useTranslationsSelector,
  useTranslationsActions,
} from './context/TranslationsContext';
import {
  AfterCommand,
  DeletableKeyWithTranslationsModelType,
  EditMode,
} from './context/types';

type LanguageModel = components['schemas']['LanguageModel'];

type Props = {
  keyData: DeletableKeyWithTranslationsModelType;
  language: LanguageModel;
  onSaveSuccess?: (val: string) => void;
  cellRef: React.RefObject<HTMLElement>;
};

export const useTranslationCell = ({
  keyData,
  language,
  onSaveSuccess,
  cellRef,
}: Props) => {
  const {
    setEditValue,
    setEditValueString,
    registerElement,
    unregisterElement,
    setEdit,
    changeField,
    setEditForce,
    setTranslationState,
    updateEdit,
  } = useTranslationsActions();

  const { satisfiesLanguageAccess } = useProjectPermissions();

  const keyId = keyData.keyId;
  const langTag = language.tag;

  const cursor = useTranslationsSelector((v) => {
    return v.cursor?.keyId === keyId && v.cursor.language === language.tag
      ? v.cursor
      : undefined;
  });

  const baseLanguage = useTranslationsSelector((c) =>
    c.languages?.find((l) => l.base)
  );

  const isEditingRow = Boolean(cursor?.keyId === keyId);
  const isEditing = Boolean(isEditingRow && cursor?.language === langTag);

  const value =
    (isEditing && cursor?.value.variants[cursor.activeVariant ?? 'other']) ||
    '';

  useEffect(() => {
    registerElement({ keyId, language: langTag, ref: cellRef.current! });
    return () => {
      unregisterElement({ keyId, language: langTag, ref: cellRef.current! });
    };
  }, [cellRef.current, keyId, langTag]);

  const handleOpen = (mode?: EditMode) => {
    setEdit({
      keyId,
      language: langTag,
      mode,
    });
  };

  const handleSave = (after?: AfterCommand) => {
    changeField({
      after,
      onSuccess: () => onSaveSuccess?.(value),
    });
  };

  const handleInsertBase = () => {
    if (!baseLanguage?.tag) {
      return;
    }
    const baseText = keyData.translations[baseLanguage.tag].text;

    let baseVariant: string | undefined;
    if (cursor?.activeVariant) {
      const variants = getTolgeeFormat(
        baseText || '',
        keyData.keyIsPlural
      )?.variants;
      baseVariant = variants?.[cursor.activeVariant] ?? variants?.['other'];
    } else {
      baseVariant = baseText;
    }

    if (baseVariant) {
      setEditValueString(baseVariant);
    }
  };

  const handleClose = (force = false) => {
    if (force) {
      setEditForce(undefined);
    } else {
      setEdit(undefined);
    }
  };

  const translation = langTag ? keyData?.translations[langTag] : undefined;

  const setState = () => {
    if (!translation) {
      return;
    }
    const nextState = TRANSLATION_STATES[translation.state]?.next;
    if (nextState) {
      setTranslationState({
        state: nextState,
        keyId,
        translationId: translation!.id,
        language: langTag!,
      });
    }
  };

  function setVariant(activeVariant: string | undefined) {
    updateEdit({ activeVariant });
  }

  const canChangeState = satisfiesLanguageAccess(
    'translations.state-edit',
    language.id
  );

  const disabled = translation?.state === 'DISABLED';
  const editEnabled =
    satisfiesLanguageAccess('translations.edit', language.id) && !disabled;

  return {
    keyId,
    language,
    handleOpen,
    handleClose,
    handleSave,
    handleInsertBase,
    setEditValue,
    setEditValueString,
    setState,
    setVariant,
    value,
    editVal: isEditing ? cursor : undefined,
    isEditing,
    isEditingRow,
    autofocus: true,
    keyData,
    canChangeState,
    editEnabled,
    translation,
    disabled,
  };
};
